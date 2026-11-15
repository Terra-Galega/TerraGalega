package com.example.terragalegamovile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.terragalegamovile.data.ApiClient
import com.example.terragalegamovile.data.AppStorage
import com.example.terragalegamovile.data.CartStore
import com.example.terragalegamovile.data.CheckoutDraft
import com.example.terragalegamovile.data.ClientUi
import com.example.terragalegamovile.data.DeliveryNotifier
import com.example.terragalegamovile.data.MapService
import com.example.terragalegamovile.data.Session
import com.example.terragalegamovile.ui.CartSheet
import com.example.terragalegamovile.ui.CategoryScreen
import com.example.terragalegamovile.ui.CheckoutConfirmScreen
import com.example.terragalegamovile.ui.CompleteProfileScreen
import com.example.terragalegamovile.ui.CheckoutScreen
import com.example.terragalegamovile.ui.ClientHomeScreen
import com.example.terragalegamovile.ui.ClientScreen
import com.example.terragalegamovile.ui.CourierScreen
import com.example.terragalegamovile.ui.LoginScreen
import com.example.terragalegamovile.ui.ProductScreen
import com.example.terragalegamovile.ui.ProfileScreen
import com.example.terragalegamovile.ui.components.ProfileNav
import com.example.terragalegamovile.ui.theme.TerraGalegaMovileTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa el almacenamiento y recupera una sesión vigente.
        AppStorage.init(applicationContext)
        MapService.init(applicationContext)
        Session.restore()

        CartStore.bind(
            Session.user?.takeIf { it.role == "CLIENT" }?.id
        )

        // Avisos de "tu pedido salió" y "ya va llegando" (notificación + vibración)
        DeliveryNotifier.start(applicationContext)
        handleNotificationIntent(intent)

        enableEdgeToEdge()

        setContent {
            TerraGalegaMovileTheme {

                // Sincroniza el carrito mientras la actividad está visible.
                LaunchedEffect(lifecycle) {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        while (isActive) {
                            delay(6_000)
                            CartStore.refresh()
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        AppNav(lifecycle = lifecycle)
                    }
                }
            }
        }
    }

    // La app ya estaba abierta (singleTask) y el usuario tocó una notificación
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    // Tocar la notificación de domicilio abre la pantalla de seguimiento
    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(DeliveryNotifier.EXTRA_OPEN_TRACKING, false)) {
            DeliveryNotifier.openTrackingRequest.value = true
            intent.removeExtra(DeliveryNotifier.EXTRA_OPEN_TRACKING)
        }
    }
}

@Composable
fun AppNav(lifecycle: Lifecycle) {
    val nav = rememberNavController()

    // Las pantallas se actualizan cuando cambia la sesión.
    val session by Session.state.collectAsState()

    var showCart by remember { mutableStateOf(false) }

    // Se calcula una sola vez para inicializar la navegación.
    val startDestination = remember {
        when (session?.role) {
            "CLIENT" -> "client"
            "DELIVERY_PERSON" -> "courier"
            else -> "login"
        }
    }

    // El efecto de cierre de sesión se encarga de limpiar y navegar.
    val logout: () -> Unit = {
        Session.user = null
    }

    // Vuelve a la pestaña Carta del inicio del cliente.
    val goMenu: () -> Unit = {
        showCart = false
        ClientUi.tab = 0
        nav.popBackStack("client", false)
    }

    // La inicial de la barra superior abre el perfil.
    // El callback consulta la sesión vigente al pulsar el botón.
    DisposableEffect(Unit) {
        ProfileNav.open = {
            if (
                Session.user?.role == "CLIENT" &&
                nav.currentDestination?.route != "profile"
            ) {
                nav.navigate("profile")
            }
        }

        onDispose {
            ProfileNav.open = null
        }
    }

    // Cierre manual o automático:
    // limpia los datos de pantalla y elimina la navegación privada.
    LaunchedEffect(session == null) {
        if (session == null) {
            showCart = false
            CartStore.bind(null)
            CheckoutDraft.reset()
            ClientUi.tab = 0
            ClientUi.paid = false

            if (nav.currentDestination?.route != "login") {
                nav.navigate("login") {
                    popUpTo(nav.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }
    }

    // Comprueba la sesión mientras la actividad está visible.
    // Al volver a la app se comprueba de nuevo inmediatamente.
    LaunchedEffect(session?.token, lifecycle) {
        if (session == null) return@LaunchedEffect

        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                // Detecta el vencimiento incluso sin conexión.
                if (Session.validToken() == null) {
                    break
                }

                try {
                    ApiClient.api.checkSession().close()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // ApiClient procesa los 401 y elimina la sesión.
                    // Un fallo de conexión o un error del servidor
                    // no deben cerrar la sesión por sí solos.
                }

                delay(15_000)
            }
        }
    }

    // Android 13+: los avisos de domicilio necesitan el permiso de notificaciones.
    // Se pide una vez, cuando entra un cliente.
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(session?.role) {
        if (session?.role == "CLIENT" &&
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Se tocó una notificación de domicilio: abre el seguimiento del cliente
    val openTracking by DeliveryNotifier.openTrackingRequest.collectAsState()

    LaunchedEffect(openTracking, session?.role) {
        if (!openTracking) return@LaunchedEffect

        if (session?.role == "CLIENT") {
            if (nav.currentDestination?.route != "tracking") {
                nav.navigate("tracking") { launchSingleTop = true }
            }
            DeliveryNotifier.openTrackingRequest.value = false
        } else if (session == null) {
            // Sin sesión no hay pedido que mostrar: se descarta la solicitud
            DeliveryNotifier.openTrackingRequest.value = false
        }
    }

    NavHost(
        navController = nav,
        startDestination = startDestination
    ) {

        composable("login") {
            LoginScreen { user ->
                // LoginScreen ya guarda el usuario en Session.user.
                CartStore.bind(
                    user.takeIf { it.role == "CLIENT" }?.id
                )

                val destination = when (user.role) {
                    "CLIENT" -> "client"
                    "DELIVERY_PERSON" -> "courier"
                    else -> null
                }

                if (destination != null) {
                    nav.navigate(destination) {
                        popUpTo("login") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                } else {
                    Session.user = null
                }
            }
        }

        composable("client") {
            session?.let { user ->
                // Cuenta creada con Google sin terminar: primero completa su perfil.
                // Al guardar cambia Session.user y aquí se muestra el inicio.
                if (user.profileComplete == false) {
                    CompleteProfileScreen(user = user, onLogout = logout)
                    return@let
                }
                ClientHomeScreen(
                    user = user,
                    onLogout = logout,
                    onOpenProduct = { id ->
                        nav.navigate("product/$id")
                    },
                    onOpenCategory = { name ->
                        nav.navigate("category/${Uri.encode(name)}")
                    },
                    onOpenCart = {
                        showCart = true
                    },
                    onTrack = {
                        nav.navigate("tracking")
                    }
                )
            }
        }

        composable(
            route = "category/{name}",
            arguments = listOf(
                navArgument("name") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val name = entry.arguments?.getString("name")
            val user = session

            if (name != null && user != null) {
                CategoryScreen(
                    user = user,
                    category = name,
                    onBack = {
                        nav.popBackStack()
                    },
                    onOpenProduct = { id ->
                        nav.navigate("product/$id")
                    },
                    onOpenCart = {
                        showCart = true
                    },
                    onLogout = logout
                )
            }
        }

        composable(
            route = "product/{id}",
            arguments = listOf(
                navArgument("id") {
                    type = NavType.IntType
                }
            )
        ) { entry ->
            val id = entry.arguments?.getInt("id")
            val user = session

            if (id != null && user != null) {
                ProductScreen(
                    user = user,
                    productId = id,
                    onBack = {
                        nav.popBackStack()
                    },
                    onOpenProduct = { other ->
                        nav.navigate("product/$other")
                    },
                    onOpenCart = {
                        showCart = true
                    },
                    onLogout = logout
                )
            }
        }

        composable("tracking") {
            session?.let { user ->
                ClientScreen(
                    user,
                    logout,
                    onBack = {
                        nav.popBackStack()
                    }
                )
            }
        }

        composable("checkout") {
            session?.let { user ->
                CheckoutScreen(
                    user = user,
                    onLogout = logout,
                    onContinue = {
                        nav.navigate("checkout/confirm")
                    },
                    onExit = goMenu
                )
            }
        }

        composable("checkout/confirm") {
            session?.let { user ->
                CheckoutConfirmScreen(
                    user = user,
                    onLogout = logout,
                    onEdit = {
                        nav.popBackStack()
                    },
                    onDone = {
                        nav.popBackStack("client", false)
                    },
                    onExit = goMenu
                )
            }
        }

        composable("profile") {
            session?.let { user ->
                ProfileScreen(
                    user = user,
                    onBack = {
                        nav.popBackStack()
                    },
                    onLogout = logout
                )
            }
        }

        composable("courier") {
            session?.let { user ->
                CourierScreen(
                    user,
                    logout
                )
            }
        }
    }

    // El carrito solo puede mostrarse con una sesión de cliente.
    if (showCart && session?.role == "CLIENT") {
        CartSheet(
            onClose = {
                showCart = false
            },
            onCheckout = {
                showCart = false
                nav.navigate("checkout")
            },
            onGoMenu = goMenu
        )
    }
}