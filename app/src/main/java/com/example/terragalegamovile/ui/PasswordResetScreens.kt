package com.example.terragalegamovile.ui

import com.example.terragalegamovile.ui.theme.ErrorText

import com.example.terragalegamovile.ui.components.ButtonStack

import com.example.terragalegamovile.data.t

import com.example.terragalegamovile.ui.components.ErrorNotice

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.terragalegamovile.data.ApiClient
import com.example.terragalegamovile.data.EmailRequest
import com.example.terragalegamovile.data.ResetPasswordRequest
import com.example.terragalegamovile.data.apiError
import com.example.terragalegamovile.data.toUserMessage
import com.example.terragalegamovile.ui.components.TerraButton
import com.example.terragalegamovile.ui.components.TerraPasswordField
import com.example.terragalegamovile.ui.components.TerraTextField
import com.example.terragalegamovile.ui.theme.Burgundy
import com.example.terragalegamovile.ui.theme.Charcoal
import com.example.terragalegamovile.ui.theme.Forest
import com.example.terragalegamovile.ui.theme.Terra
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private val RESET_EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+$")

// Token del enlace del correo cuando el usuario lo abre con la app
// (MainActivity lo pone aquí; la pantalla de login lo consume)
object PasswordResetLink {
    val token = MutableStateFlow<String?>(null)

    // Acepta el enlace completo (…/reset-password?token=XYZ) o solo el token
    fun tokenFrom(text: String): String? {
        val raw = text.trim()
        if (raw.isEmpty()) return null
        if (!raw.contains("token=")) return raw.takeIf { !it.contains(' ') && !it.contains('/') }
        return try {
            Uri.parse(raw).getQueryParameter("token")?.trim()?.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }
}

// "¿Olvidaste tu contraseña?": igual que /forgot-password en la web. El back envía
// un enlace al correo; al tocarlo en el teléfono se puede abrir con la app.
// También se puede pegar el enlace aquí.
@Composable
fun ForgotPasswordContent(
    initialEmail: String,
    onBack: () -> Unit,
    onToken: (String) -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var sending by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var link by remember { mutableStateOf("") }
    var linkError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    BackHandler(onBack = onBack)

    fun submit() {
        if (sending) return
        val target = email.trim()
        if (!RESET_EMAIL_REGEX.matches(target)) {
            error = t("El correo electrónico no es válido. Debe tener algo antes y después del @.")
            return
        }
        error = null
        sending = true
        scope.launch {
            try {
                ApiClient.api.forgotPassword(EmailRequest(target))
                sent = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.apiError()?.message ?: e.toUserMessage()
            } finally {
                sending = false
            }
        }
    }

    Column {
        Text(t("Recuperar contraseña"), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Charcoal)
        Spacer(Modifier.height(6.dp))

        if (!sent) {
            Text(
                t("Escribe el correo de tu cuenta y te enviaremos un enlace para elegir una contraseña nueva."),
                fontSize = 14.sp,
                color = Charcoal.copy(alpha = 0.65f)
            )
            Spacer(Modifier.height(16.dp))
            TerraTextField(
                label = t("Correo electrónico"),
                value = email,
                onValueChange = { email = it },
                placeholder = "correo@ejemplo.com",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            error?.let {
                Spacer(Modifier.height(12.dp))
                ErrorNotice(it)
            }
            Spacer(Modifier.height(20.dp))
            ButtonStack {
                TerraButton(
                    text = if (sending) t("Enviando...") else t("Enviar enlace"),
                    onClick = { submit() },
                    enabled = !sending && email.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
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
                    t("Si hay una cuenta con {}, te enviamos un enlace para recuperar tu contraseña. ", email.trim()) +
                        t("Ábrelo desde este teléfono y elige \"TerraGalega\" para cambiarla aquí."),
                    fontSize = 14.sp,
                    color = Charcoal.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            // Por si el enlace se abrió en otro lado: se puede pegar aquí
            TerraTextField(
                label = t("¿Tienes el enlace? Pégalo aquí"),
                value = link,
                onValueChange = { link = it; linkError = null },
                placeholder = "https://…/reset-password?token=…"
            )
            linkError?.let {
                Spacer(Modifier.height(8.dp))
                ErrorNotice(it)
            }
            Spacer(Modifier.height(16.dp))
            ButtonStack {
                TerraButton(
                    text = t("Continuar"),
                    onClick = {
                        val token = PasswordResetLink.tokenFrom(link)
                        if (token == null) linkError = t("Ese enlace no es válido.") else onToken(token)
                    },
                    enabled = link.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                t("Reenviar enlace"),
                color = Terra,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .clickable(enabled = !sending) { sent = false }
                    .padding(vertical = 4.dp)
            )
        }

        Text(
            t("Volver a iniciar sesión"),
            color = Charcoal.copy(alpha = 0.6f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .padding(top = 14.dp)
                .clickable(onClick = onBack)
                .padding(vertical = 4.dp)
        )
    }
}

// Elegir la contraseña nueva: igual que /reset-password en la web
@Composable
fun ResetPasswordContent(token: String, onDone: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // Contraseña cambiada (done) o enlace vencido/usado (invalid)
    var doneMessage by remember { mutableStateOf<String?>(null) }
    var invalidMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    BackHandler(onBack = onDone)

    fun submit() {
        if (saving) return
        error = when {
            password.length < 6 -> t("La nueva contraseña debe tener al menos 6 caracteres.")
            password.length > 70 -> t("La nueva contraseña es demasiado larga.")
            password != confirm -> t("Las contraseñas no coinciden.")
            else -> null
        }
        if (error != null) return
        saving = true
        scope.launch {
            try {
                doneMessage = ApiClient.api.resetPassword(ResetPasswordRequest(token, password)).message
                    ?: t("Tu contraseña se cambió. Ya puedes iniciar sesión.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val body = e.apiError()
                val message = body?.message
                // Token vencido, usado o inválido: ya no sirve reintentar con este enlace
                if (message != null && message.contains("enlace", ignoreCase = true)) {
                    invalidMessage = message
                } else {
                    error = message ?: e.toUserMessage()
                }
            } finally {
                saving = false
            }
        }
    }

    Column {
        Text(t("Nueva contraseña"), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Charcoal)
        Spacer(Modifier.height(6.dp))

        when {
            doneMessage != null -> {
                Text(doneMessage ?: "", fontSize = 14.sp, color = Forest)
                Spacer(Modifier.height(20.dp))
                ButtonStack {
                    TerraButton(text = t("Iniciar sesión"), onClick = onDone, modifier = Modifier.fillMaxWidth())
                }
            }
            invalidMessage != null -> {
                ErrorNotice(invalidMessage ?: "")
                Spacer(Modifier.height(20.dp))
                ButtonStack {
                    TerraButton(text = t("Volver"), onClick = onDone, modifier = Modifier.fillMaxWidth())
                }
            }
            else -> {
                Text(t("Escribe tu nueva contraseña dos veces."), fontSize = 14.sp, color = Charcoal.copy(alpha = 0.65f))
                Spacer(Modifier.height(16.dp))
                TerraPasswordField(t("Nueva contraseña"), password, { password = it; error = null })
                Spacer(Modifier.height(16.dp))
                TerraPasswordField(t("Confirmar contraseña"), confirm, { confirm = it; error = null })
                Spacer(Modifier.height(8.dp))
                Text(
                    error ?: t("Mínimo 6 caracteres."),
                    color = if (error != null) ErrorText else Charcoal.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(20.dp))
                ButtonStack {
                    TerraButton(
                        text = if (saving) t("Guardando...") else t("Cambiar contraseña"),
                        onClick = { submit() },
                        enabled = !saving && password.isNotEmpty() && confirm.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(
                    t("Cancelar"),
                    color = Charcoal.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .clickable(onClick = onDone)
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}
