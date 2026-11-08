package com.example.terragalegamovile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.terragalegamovile.data.Session
import com.example.terragalegamovile.ui.LoginScreen
import com.example.terragalegamovile.ui.theme.TerraGalegaMovileTheme
import com.example.terragalegamovile.ui.CourierScreen
import com.example.terragalegamovile.ui.ClientScreen
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TerraGalegaMovileTheme {
                Surface(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                    AppNav()
                }
            }
        }
    }
}

@Composable
fun AppNav() {
    val nav = rememberNavController()

    val logout = {
        Session.user = null
        nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } }
    }

    NavHost(navController = nav, startDestination = "login") {
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
        }    }
}

// Pantalla de prueba, se reemplaza en los siguientes pasos
@Composable
fun TempScreen(title: String, onLogout: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text("Hola, ${Session.user?.name} ${Session.user?.lastName}")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onLogout) { Text("Cerrar sesión") }
    }
}