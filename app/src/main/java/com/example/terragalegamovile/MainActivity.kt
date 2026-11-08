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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recupera la sesión guardada antes de dibujar nada
        AppStorage.init(applicationContext)
        MapService.init(applicationContext)
        Session.restore()
        enableEdgeToEdge()
        setContent {
            TerraGalegaMovileTheme {
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

    // Si ya hay sesión guardada, abre directo la pantalla de su rol
    val startDestination = remember {
        when (Session.user?.role) {
            "CLIENT" -> "client"
            "DELIVERY_PERSON" -> "courier"
            else -> "login"
        }
    }

    val logout = {
        Session.user = null
        nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } }
    }

    NavHost(navController = nav, startDestination = startDestination) {
        composable("login") {
            LoginScreen { user ->
                val destination = if (user.role == "CLIENT") "client" else "courier"
                nav.navigate(destination) { popUpTo("login") { inclusive = true } }
            }
        }
        composable("client") {
            Session.user?.let { ClientScreen(it, logout) }
        }
        composable("courier") {
            Session.user?.let { CourierScreen(it, logout) }
        }
    }
}