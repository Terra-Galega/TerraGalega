package com.example.terragalegamovile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.Color
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
import com.example.terragalegamovile.data.ClientCreate
import com.example.terragalegamovile.data.LoginRequest
import com.example.terragalegamovile.data.LoginResponse
import com.example.terragalegamovile.data.NewClientAddress
import com.example.terragalegamovile.data.Session
import com.example.terragalegamovile.data.toUserMessage
import com.example.terragalegamovile.ui.components.GlassCard
import com.example.terragalegamovile.ui.components.TerraButton
import com.example.terragalegamovile.ui.components.TerraLogo
import com.example.terragalegamovile.ui.components.TerraTextField
import com.example.terragalegamovile.ui.theme.Burgundy
import com.example.terragalegamovile.ui.theme.Charcoal
import com.example.terragalegamovile.ui.theme.Terra
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val SIGNUP_EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+$")

@Composable
fun LoginScreen(onLoggedIn: (LoginResponse) -> Unit) {
    var signup by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    // Solo para crear cuenta
    var name by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.toUserMessage()
            } finally {
                loading = false
            }
        }
    }

    // Mismas validaciones que el registro de la web
    fun register() {
        val n = name.trim()
        val l = lastName.trim()
        val em = email.trim()
        val ph = phone.trim()
        val ad = address.trim()
        when {
            n.isEmpty() || l.isEmpty() || em.isEmpty() || password.isBlank() || ph.isEmpty() || ad.isEmpty() ->
                error = "Todos los campos son obligatorios."
            !SIGNUP_EMAIL_REGEX.matches(em) ->
                error = "El correo electrónico no es válido. Debe tener algo antes y después del @."
            else -> {
                loading = true
                error = null
                scope.launch {
                    try {
                        val created = ApiClient.api.register(
                            ClientCreate(
                                name = n,
                                lastName = l,
                                email = em,
                                password = password,
                                phone = ph,
                                addresses = listOf(NewClientAddress(ad))
                            )
                        )
                        // Entra directo con la cuenta recién creada, como la web
                        val user = LoginResponse(
                            id = created.id,
                            name = created.name,
                            lastName = created.lastName,
                            role = "CLIENT"
                        )
                        Session.user = user
                        onLoggedIn(user)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        error = e.toUserMessage()
                    } finally {
                        loading = false
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(if (signup) 32.dp else 72.dp))
        TerraLogo(scale = 2.2f)
        Spacer(Modifier.height(12.dp))
        Text(
            if (signup) "CREA TU CUENTA" else "BIENVENIDO DE VUELTA",
            fontSize = 15.sp,
            letterSpacing = 3.sp,
            color = Charcoal.copy(alpha = 0.38f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            AuthTabs(signup = signup) {
                signup = it
                error = null
            }
            Spacer(Modifier.height(20.dp))

            if (signup) {
                TerraTextField(
                    label = "Nombre",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Marta"
                )
                Spacer(Modifier.height(16.dp))
                TerraTextField(
                    label = "Apellido",
                    value = lastName,
                    onValueChange = { lastName = it },
                    placeholder = "Souto"
                )
                Spacer(Modifier.height(16.dp))
            }

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

            if (signup) {
                Spacer(Modifier.height(16.dp))
                TerraTextField(
                    label = "Teléfono",
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = "3001234567",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Spacer(Modifier.height(16.dp))
                TerraTextField(
                    label = "Dirección de entrega",
                    value = address,
                    onValueChange = { address = it.take(120) },
                    placeholder = "Calle 8 Sur #68C-20"
                )
            }

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = Burgundy, fontSize = 14.sp)
            }

            Spacer(Modifier.height(24.dp))
            if (signup) {
                TerraButton(
                    text = if (loading) "Creando cuenta..." else "Crear cuenta",
                    onClick = { register() },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
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
}

// Pestañas "Iniciar sesión" / "Crear cuenta", como en la web
@Composable
private fun AuthTabs(signup: Boolean, onChange: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Charcoal.copy(alpha = 0.06f))
            .padding(4.dp)
    ) {
        listOf("Iniciar sesión" to false, "Crear cuenta" to true).forEach { (label, isSignup) ->
            val active = signup == isSignup
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (active) Color.White else Color.Transparent)
                    .clickable { onChange(isSignup) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (active) Terra else Charcoal.copy(alpha = 0.55f)
                )
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