package com.example.terragalegamovile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.terragalegamovile.data.ApiClient
import com.example.terragalegamovile.data.LoginRequest
import com.example.terragalegamovile.data.LoginResponse
import com.example.terragalegamovile.data.Session
import com.example.terragalegamovile.data.toUserMessage
import com.example.terragalegamovile.ui.components.GlassCard
import com.example.terragalegamovile.ui.components.TerraButton
import com.example.terragalegamovile.ui.components.TerraLogo
import com.example.terragalegamovile.ui.components.TerraTextField
import com.example.terragalegamovile.ui.theme.Burgundy
import com.example.terragalegamovile.ui.theme.Charcoal
import com.example.terragalegamovile.ui.theme.TextLabel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoggedIn: (LoginResponse) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun login() {
        loading = true
        error = null
        scope.launch {
            try {
                val user = ApiClient.api.login(LoginRequest(email.trim(), password))
                if (user.role == "CLIENT" || user.role == "DELIVERY_PERSON") {
                    Session.user = user
                    onLoggedIn(user)
                } else {
                    error = "Esta app es solo para clientes y repartidores."
                }
            } catch (e: Exception) {
                error = e.toUserMessage()
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        TerraLogo(scale = 1.8f)
        Spacer(Modifier.height(12.dp))
        Text(
            "BIENVENIDO DE VUELTA",
            fontSize = 12.sp,
            letterSpacing = 3.sp,
            color = Charcoal.copy(alpha = 0.38f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            TerraTextField(
                label = "Correo electrónico",
                value = email,
                onValueChange = { email = it },
                placeholder = "correo@ejemplo.com",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(Modifier.height(16.dp))
            TerraTextField(
                label = "Contraseña",
                value = password,
                onValueChange = { password = it },
                placeholder = "••••••••",
                visualTransformation = PasswordVisualTransformation()
            )

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = Burgundy, fontSize = 14.sp)
            }

            Spacer(Modifier.height(24.dp))
            TerraButton(
                text = if (loading) "Entrando..." else "Iniciar sesión",
                onClick = { login() },
                enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            // Cuentas de prueba (igual que en la web)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Charcoal.copy(alpha = 0.04f))
                    .border(1.dp, Charcoal.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                DemoUser("Cliente de prueba: ", "marta.souto@terra.com") {
                    email = "marta.souto@terra.com"; password = "marta123"
                }
                DemoUser("Repartidor de prueba: ", "luisa.fernandez@terra.com") {
                    email = "luisa.fernandez@terra.com"; password = "luisa123"
                }
            }
        }
    }
}

@Composable
private fun DemoUser(title: String, email: String, onClick: () -> Unit) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Charcoal)) { append(title) }
            append(email)
        },
        fontSize = 12.sp,
        color = Charcoal.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}