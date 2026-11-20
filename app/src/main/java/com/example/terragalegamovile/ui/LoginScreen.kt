package com.example.terragalegamovile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalContext
import com.example.terragalegamovile.data.GOOGLE_WEB_CLIENT_ID
import com.example.terragalegamovile.data.GoogleLoginRequest
import com.example.terragalegamovile.data.EmailRequest
import com.example.terragalegamovile.data.apiError
import com.example.terragalegamovile.ui.theme.Forest
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import com.example.terragalegamovile.data.requestGoogleIdToken
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

    // Confirmación de correo: correo al que hay que confirmar (null = nada pendiente)
    var pendingEmail by remember { mutableStateOf<String?>(null) }
    var pendingText by remember { mutableStateOf("") }
    var resendMessage by remember { mutableStateOf<String?>(null) }
    // Segundos para poder reenviar otra vez (el back usa el mismo límite)
    var resendCooldown by remember { mutableStateOf(0) }

    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1_000)
            resendCooldown -= 1
        }
    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun login() {
        loading = true
        error = null
        scope.launch {
            try {
                val user = ApiClient.api.login(LoginRequest(email.trim(), password))
                if (user.role == "CLIENT" || user.role == "DELIVERY_PERSON" || user.role == "WAITER") {
                    Session.user = user
                    onLoggedIn(user)
                } else {
                    error = "Esta app es solo para clientes, repartidores y meseros."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val body = e.apiError()
                if (body?.code == "EMAIL_NOT_VERIFIED") {
                    // Contraseña correcta, pero falta confirmar el correo
                    pendingEmail = email.trim()
                    pendingText = body.message ?: "Debes confirmar tu correo antes de iniciar sesión."
                    resendMessage = null
                } else {
                    error = body?.message ?: e.toUserMessage()
                }
            } finally {
                loading = false
            }
        }
    }

    // Reenvía el correo de confirmación a pendingEmail
    fun resend() {
        val target = pendingEmail ?: return
        if (resendCooldown > 0 || loading) return
        loading = true
        resendMessage = null
        scope.launch {
            try {
                resendMessage = ApiClient.api.resendVerification(EmailRequest(target)).message
                    ?: "Te enviamos un nuevo correo."
                resendCooldown = 60
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                resendMessage = e.toUserMessage()
            } finally {
                loading = false
            }
        }
    }

    // "Continuar con Google": Google da el ID token y el back crea la cuenta o
    // inicia sesión. Si faltan datos, la app muestra "Completa tu perfil".
    fun googleLogin() {
        loading = true
        error = null
        scope.launch {
            try {
                val idToken = requestGoogleIdToken(context) ?: return@launch // la cerró
                val user = ApiClient.api.googleLogin(GoogleLoginRequest(idToken))
                if (user.role == "CLIENT") {
                    Session.user = user
                    onLoggedIn(user)
                } else {
                    error = "Esta app es solo para clientes, repartidores y meseros."
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
                        // El back crea la cuenta y envía un correo de confirmación.
                        // No hay sesión todavía: entra después de confirmar.
                        val result = ApiClient.api.register(
                            ClientCreate(
                                name = n,
                                lastName = l,
                                email = em,
                                password = password,
                                phone = ph,
                                addresses = listOf(NewClientAddress(ad))
                            )
                        )
                        pendingEmail = em
                        pendingText = result.message ?: "Cuenta creada. Te enviamos un correo para confirmarla."
                        resendMessage = null
                        resendCooldown = 60
                        // Al volver al login ya tiene el correo escrito
                        signup = false
                        password = ""
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
                pendingEmail = null
            }
            Spacer(Modifier.height(20.dp))

            // Revisa tu correo: tras registrarse o al intentar entrar sin confirmar
            pendingEmail?.let { target ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Forest.copy(alpha = 0.08f))
                        .border(1.dp, Forest.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text("Revisa tu correo", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Charcoal)
                    Text(
                        "$pendingText\n\nEnviamos el enlace a $target. Ábrelo para confirmar tu cuenta y luego inicia sesión aquí.",
                        fontSize = 14.sp,
                        color = Charcoal.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    resendMessage?.let {
                        Text(it, fontSize = 13.sp, color = Forest, modifier = Modifier.padding(top = 8.dp))
                    }
                    Text(
                        if (resendCooldown > 0) "Reenviar correo (${resendCooldown}s)" else "Reenviar correo",
                        color = if (resendCooldown > 0) Charcoal.copy(alpha = 0.4f) else Terra,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .clickable(enabled = resendCooldown == 0 && !loading) { resend() }
                            .padding(vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

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
                GoogleButton("Registrarse con Google", enabled = !loading) { googleLogin() }
            } else {
                TerraButton(
                    text = if (loading) "Entrando..." else "Iniciar sesión",
                    onClick = { login() },
                    enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                GoogleButton("Continuar con Google", enabled = !loading) { googleLogin() }

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
                    DemoUser("Cliente de prueba: ", "marta.souto@outlook.com") {
                        email = "marta.souto@outlook.com"; password = "marta123"
                    }
                    DemoUser("Repartidor de prueba: ", "luisa.fernandez@terra.com") {
                        email = "luisa.fernandez@terra.com"; password = "luisa123"
                    }
                    DemoUser("Mesero de prueba: ", "andres.gomez@terra.com") {
                        email = "andres.gomez@terra.com"; password = "andres123"
                    }
                }
            }
        }
    }
}

// Separador "o" y botón de Google. No aparece si falta GOOGLE_WEB_CLIENT_ID.
@Composable
private fun GoogleButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    if (GOOGLE_WEB_CLIENT_ID.isBlank()) return
    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.weight(1f).height(1.dp).background(Charcoal.copy(alpha = 0.12f)))
        Text("o", fontSize = 12.sp, color = Charcoal.copy(alpha = 0.45f), modifier = Modifier.padding(horizontal = 12.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Charcoal.copy(alpha = 0.12f)))
    }
    Spacer(Modifier.height(16.dp))
    val shape = RoundedCornerShape(50)
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, Charcoal.copy(alpha = 0.2f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp)
    ) {
        // "G" con los colores de Google
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Color(0xFF4285F4), fontWeight = FontWeight.Bold)) { append("G") }
            },
            fontSize = 18.sp
        )
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Charcoal.copy(alpha = if (enabled) 0.85f else 0.4f))
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