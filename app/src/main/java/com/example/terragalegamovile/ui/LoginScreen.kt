package com.example.terragalegamovile.ui

import com.example.terragalegamovile.ui.theme.Selected

import com.example.terragalegamovile.ui.components.ButtonStack

import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.draw.alpha
import com.example.terragalegamovile.data.t

import com.example.terragalegamovile.ui.components.ErrorNotice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
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
import com.example.terragalegamovile.ui.components.TerraPasswordField
import com.example.terragalegamovile.ui.components.TerraTextField
import androidx.compose.runtime.collectAsState
import com.example.terragalegamovile.ui.theme.Burgundy
import com.example.terragalegamovile.ui.theme.Charcoal
import com.example.terragalegamovile.ui.theme.Terra
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val SIGNUP_EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+$")

@Composable
fun LoginScreen(onLoggedIn: (LoginResponse) -> Unit) {
    var signup by remember { mutableStateOf(false) }
    // Recuperar contraseña: formulario del correo, y token del enlace para elegir la nueva
    var forgot by remember { mutableStateOf(false) }
    val linkToken by PasswordResetLink.token.collectAsState()
    var pastedToken by remember { mutableStateOf<String?>(null) }
    val resetToken = linkToken ?: pastedToken

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
                    error = t("Esta app es solo para clientes, repartidores y meseros.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val body = e.apiError()
                if (body?.code == "EMAIL_NOT_VERIFIED") {
                    // Contraseña correcta, pero falta confirmar el correo
                    pendingEmail = email.trim()
                    pendingText = body.message ?: t("Debes confirmar tu correo antes de iniciar sesión.")
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
                    ?: t("Te enviamos un nuevo correo.")
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
                    error = t("Esta app es solo para clientes, repartidores y meseros.")
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
                error = t("Todos los campos son obligatorios.")
            !SIGNUP_EMAIL_REGEX.matches(em) ->
                error = t("El correo electrónico no es válido. Debe tener algo antes y después del @.")
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
                        pendingText = result.message ?: t("Cuenta creada. Te enviamos un correo para confirmarla.")
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
        Spacer(Modifier.height(if (signup) 24.dp else 48.dp))
        // Logo de Terra Galega (el pulpo grabado)
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(com.example.terragalegamovile.R.drawable.logo_octopus),
            contentDescription = null,
            modifier = Modifier.size(if (signup) 96.dp else 150.dp)
        )
        Spacer(Modifier.height(8.dp))
        TerraLogo(scale = 2.2f)
        Spacer(Modifier.height(12.dp))
        Text(
            if (signup) t("CREA TU CUENTA") else t("BIENVENIDO DE VUELTA"),
            fontSize = 15.sp,
            letterSpacing = 3.sp,
            color = Charcoal.copy(alpha = 0.38f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        // Idioma de la app y modo oscuro
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.example.terragalegamovile.ui.components.LanguagePicker()
            Spacer(Modifier.width(8.dp))
            com.example.terragalegamovile.ui.components.DarkModeToggle()
        }
        Spacer(Modifier.height(20.dp))

        if (resetToken != null) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                ResetPasswordContent(token = resetToken) {
                    PasswordResetLink.token.value = null
                    pastedToken = null
                    forgot = false
                    signup = false
                    password = ""
                }
            }
            return@Column
        }
        if (forgot) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                ForgotPasswordContent(
                    initialEmail = email,
                    onBack = { forgot = false },
                    onToken = { pastedToken = it }
                )
            }
            return@Column
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            AuthTabs(signup = signup) {
                signup = it
                error = null
                pendingEmail = null
            }
            Spacer(Modifier.height(20.dp))

            // Acceso con Google encima del formulario, como en la web (oculto mientras
            // se espera la confirmación del correo)
            if (pendingEmail == null) {
                GoogleButton(
                    if (signup) t("Registrarse con Google") else t("Continuar con Google"),
                    enabled = !loading
                ) { googleLogin() }
            }

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
                    Text(t("Revisa tu correo"), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Charcoal)
                    Text(
                        t("{}\n\nEnviamos el enlace a {}. Ábrelo para confirmar tu cuenta y luego inicia sesión aquí.", pendingText, target),
                        fontSize = 14.sp,
                        color = Charcoal.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    resendMessage?.let {
                        Text(it, fontSize = 13.sp, color = Forest, modifier = Modifier.padding(top = 8.dp))
                    }
                    Text(
                        if (resendCooldown > 0) t("Reenviar correo ({}s)", resendCooldown) else t("Reenviar correo"),
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
                    label = t("Nombre"),
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Marta"
                )
                Spacer(Modifier.height(16.dp))
                TerraTextField(
                    label = t("Apellido"),
                    value = lastName,
                    onValueChange = { lastName = it },
                    placeholder = "Souto"
                )
                Spacer(Modifier.height(16.dp))
            }

            TerraTextField(
                label = t("Correo electrónico"),
                value = email,
                onValueChange = { email = it },
                placeholder = "correo@ejemplo.com",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(Modifier.height(16.dp))
            TerraPasswordField(
                label = t("Contraseña"),
                value = password,
                onValueChange = { password = it }
            )
            if (!signup) {
                Text(
                    t("¿Olvidaste tu contraseña?"),
                    color = Terra,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp)
                        .clickable(enabled = !loading) { forgot = true; error = null }
                        .padding(vertical = 4.dp)
                )
            }

            if (signup) {
                Spacer(Modifier.height(16.dp))
                TerraTextField(
                    label = t("Teléfono"),
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = "3001234567",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Spacer(Modifier.height(16.dp))
                TerraTextField(
                    label = t("Dirección de entrega"),
                    value = address,
                    onValueChange = { address = it.take(120) },
                    placeholder = "Calle 8 Sur #68C-20"
                )
            }

            error?.let {
                Spacer(Modifier.height(12.dp))
                ErrorNotice(it)
            }

            Spacer(Modifier.height(24.dp))
            if (signup) {
                ButtonStack {
                    TerraButton(
                        text = if (loading) t("Creando cuenta...") else t("Crear cuenta"),
                        onClick = { register() },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                ButtonStack {
                    TerraButton(
                        text = if (loading) t("Entrando...") else t("Iniciar sesión"),
                        onClick = { login() },
                        enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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
                    DemoUser(t("Cliente de prueba: "), "marta.souto@outlook.com") {
                        email = "marta.souto@outlook.com"; password = "marta123"
                    }
                    DemoUser(t("Repartidor de prueba: "), "luisa.fernandez@terra.com") {
                        email = "luisa.fernandez@terra.com"; password = "luisa123"
                    }
                    DemoUser(t("Mesero de prueba: "), "andres.gomez@terra.com") {
                        email = "andres.gomez@terra.com"; password = "andres123"
                    }
                }
            }
        }
    }
}

// Botón de Google igual al oficial de la web (theme outline, size large, shape pill):
// blanco, borde gris de Google, logo "G" de cuatro colores a la izquierda y el texto
// centrado. Debajo, el separador "o" antes del formulario. No aparece sin GOOGLE_WEB_CLIENT_ID.
@Composable
private fun GoogleButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    if (GOOGLE_WEB_CLIENT_ID.isBlank()) return
    val shape = RoundedCornerShape(50)
    // Del ancho de su texto y centrado, como los demás botones de la app
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(44.dp)
                .clip(shape)
                .background(Selected)
                .border(1.dp, Color(0xFFDADCE0), shape)
                .clickable(enabled = enabled, onClick = onClick)
                .alpha(if (enabled) 1f else 0.6f)
                .padding(start = 14.dp, end = 22.dp)
        ) {
            GoogleLogo(Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF3C4043),
                maxLines = 1
            )
        }
    }
    // Separador "o", igual que en la web
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 22.dp)
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(Charcoal.copy(alpha = 0.12f)))
        Text(
            t("o").uppercase(),
            fontSize = 12.sp,
            letterSpacing = 1.sp,
            color = Charcoal.copy(alpha = 0.45f),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Box(Modifier.weight(1f).height(1.dp).background(Charcoal.copy(alpha = 0.12f)))
    }
}

// Logo "G" de Google con sus cuatro colores (el mismo del botón oficial)
private val GOOGLE_G = listOf(
    Color(0xFFEA4335) to "M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z",
    Color(0xFF4285F4) to "M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z",
    Color(0xFFFBBC05) to "M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z",
    Color(0xFF34A853) to "M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"
).map { (color, d) -> color to androidx.compose.ui.graphics.vector.PathParser().parsePathString(d).toPath() }

@Composable
private fun GoogleLogo(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val scale = size.minDimension / 48f
        withTransform({ scale(scale, scale, pivot = androidx.compose.ui.geometry.Offset.Zero) }) {
            GOOGLE_G.forEach { (color, path) -> drawPath(path, color) }
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
        listOf(t("Iniciar sesión") to false, t("Crear cuenta") to true).forEach { (label, isSignup) ->
            val active = signup == isSignup
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (active) Selected else Color.Transparent)
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