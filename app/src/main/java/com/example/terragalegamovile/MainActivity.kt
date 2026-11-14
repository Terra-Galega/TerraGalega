package com.example.terragalegamovile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.terragalegamovile.data.AppStorage
import com.example.terragalegamovile.data.MapService
import com.example.terragalegamovile.data.Session
import com.example.terragalegamovile.ui.ClientScreen
import com.example.terragalegamovile.ui.CourierScreen
import com.example.terragalegamovile.ui.LoginScreen
import com.example.terragalegamovile.ui.theme.TerraGalegaMovileTheme
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.LaunchedEffect
import com.example.terragalegamovile.data.CartStore
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.terragalegamovile.data.CheckoutDraft
import com.example.terragalegamovile.data.ClientUi
import com.example.terragalegamovile.ui.CartSheet
import com.example.terragalegamovile.ui.CheckoutConfirmScreen
import com.example.terragalegamovile.ui.CheckoutScreen
import com.example.terragalegamovile.ui.ClientHomeScreen
import com.example.terragalegamovile.ui.ProductScreen
import android.net.Uri
import androidx.compose.runtime.DisposableEffect
import com.example.terragalegamovile.ui.CategoryScreen
import com.example.terragalegamovile.ui.ProfileScreen
import com.example.terragalegamovile.ui.components.ProfileNav

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recupera la sesión guardada antes de dibujar nada
        AppStorage.init(applicationContext)
        MapService.init(applicationContext)
        Session.restore()
        CartStore.bind(Session.user?.takeIf { it.role == "CLIENT" }?.id)
        enableEdgeToEdge()
        setContent {
            TerraGalegaMovileTheme {
                // Si el carrito cambió en la web, aquí se refleja (también actualiza la cifra del carrito)
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(6000)
                        CartStore.refresh()
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                        AppNav()
                    }
                }
            }
        }
    }
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    var showCart by remember { mutableStateOf(false) }

    // Si ya hay sesión guardada, abre directo la pantalla de su rol
    val startDestination = remember {
        when (Session.user?.role) {
            "CLIENT" -> "client"
            "DELIVERY_PERSON" -> "courier"
            else -> "login"
        }
    }

    val logout = {
        showCart = false
        CartStore.bind(null)
        CheckoutDraft.reset()
        ClientUi.tab = 0
        ClientUi.paid = false
        Session.user = null
        nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } }
    }

    // Vuelve a la pestaña "Carta" del inicio del cliente
    val goMenu = {
        showCart = false
        ClientUi.tab = 0
        nav.popBackStack("client", false)
    }

    // La inicial de la barra de arriba abre el perfil (solo para clientes)
    DisposableEffect(Unit) {
        ProfileNav.open = {
            if (Session.user?.role == "CLIENT" && nav.currentDestination?.route != "profile") {
                nav.navigate("profile")
            }
        }
        onDispose { ProfileNav.open = null }
    }

    NavHost(navController = nav, startDestination = startDestination) {
        composable("login") {
            LoginScreen { user ->
                if (user.role == "CLIENT") CartStore.bind(user.id)
                val destination = if (user.role == "CLIENT") "client" else "courier"
                nav.navigate(destination) { popUpTo("login") { inclusive = true } }
            }
        }
        composable("client") {
            Session.user?.let { user ->
                ClientHomeScreen(
                    user = user,
                    onLogout = logout,
                    onOpenProduct = { id -> nav.navigate("product/$id") },
                    onOpenCategory = { name -> nav.navigate("category/${Uri.encode(name)}") },
                    onOpenCart = { showCart = true },
                    onTrack = { nav.navigate("tracking") }
                )
            }
        }

        composable(
            "category/{name}",
            arguments = listOf(navArgument("name") { type = NavType.StringType })
        ) { entry ->
            val name = entry.arguments?.getString("name")
            val user = Session.user
            if (name != null && user != null) {
                CategoryScreen(
                    user = user,
                    category = name,
                    onBack = { nav.popBackStack() },
                    onOpenProduct = { id -> nav.navigate("product/$id") },
                    onOpenCart = { showCart = true },
                    onLogout = logout
                )
            }
        }
        composable(
            "product/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { entry ->
            val id = entry.arguments?.getInt("id")
            val user = Session.user
            if (id != null && user != null) {
                ProductScreen(
                    user = user,
                    productId = id,
                    onBack = { nav.popBackStack() },                    onOpenProduct = { other -> nav.navigate("product/$other") },
                    onOpenCart = { showCart = true },
                    onLogout = logout
                )
            }
        }
        composable("tracking") {
            Session.user?.let { ClientScreen(it, logout, onBack = { nav.popBackStack() }) }
        }
        composable("checkout") {
            Session.user?.let { user ->
                CheckoutScreen(
                    user = user,
                    onLogout = logout,
                    onContinue = { nav.navigate("checkout/confirm") },
                    onExit = { goMenu() }
                )
            }
        }
        composable("checkout/confirm") {
            Session.user?.let { user ->
                CheckoutConfirmScreen(
                    user = user,
                    onLogout = logout,
                    onEdit = { nav.popBackStack() },
                    onDone = { nav.popBackStack("client", false) },
                    onExit = { goMenu() }
                )
            }
        }

        composable("profile") {
            Session.user?.let { user ->
                ProfileScreen(
                    user = user,
                    onBack = { nav.popBackStack() },
                    onLogout = logout
                )
            }
        }
        composable("courier") {
            Session.user?.let { CourierScreen(it, logout) }
        }
    }

    // El carrito se abre encima de cualquier pantalla del cliente
    if (showCart) {
        CartSheet(
            onClose = { showCart = false },
            onCheckout = { showCart = false; nav.navigate("checkout") },
            onGoMenu = { goMenu() }
        )
    }
}