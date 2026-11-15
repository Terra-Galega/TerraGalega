package com.example.terragalegamovile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.terragalegamovile.data.ApiClient
import com.example.terragalegamovile.data.ChangePasswordRequest
import com.example.terragalegamovile.data.ClientUpdate
import com.example.terragalegamovile.data.LoginResponse
import com.example.terragalegamovile.data.Session
import com.example.terragalegamovile.data.toUserMessage
import com.example.terragalegamovile.ui.components.BackLink
import com.example.terragalegamovile.ui.components.ButtonStyle
import com.example.terragalegamovile.ui.components.GlassCard
import com.example.terragalegamovile.ui.components.InfoLine
import com.example.terragalegamovile.ui.components.PageHeader
import com.example.terragalegamovile.ui.components.TerraButton
import com.example.terragalegamovile.ui.components.TerraDialog
import com.example.terragalegamovile.ui.components.TerraScaffold
import com.example.terragalegamovile.ui.components.TerraTextField
import com.example.terragalegamovile.ui.components.ToastPill
import com.example.terragalegamovile.ui.theme.Burgundy
import com.example.terragalegamovile.ui.theme.Charcoal
import com.example.terragalegamovile.ui.theme.Cream
import com.example.terragalegamovile.ui.theme.Playfair
import com.example.terragalegamovile.ui.theme.Terra
import com.example.terragalegamovile.ui.theme.TextSoft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+$")

// Perfil del cliente: ver, editar, cerrar sesión y eliminar cuenta (como "Tu perfil" de la web)
@Composable
fun ProfileScreen(
    user: LoginResponse,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onOpenCoupons: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var current by remember { mutableStateOf(user) }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var editing by remember { mutableStateOf(false) }
    var fName by remember { mutableStateOf("") }
    var fLast by remember { mutableStateOf("") }
    var fEmail by remember { mutableStateOf("") }
    var fPhone by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    var showPassword by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }

    // Trae el correo y el teléfono del backend
    LaunchedEffect(Unit) {
        try {
            val c = ApiClient.api.getClient()
            email = c.email
            phone = c.phone
            loaded = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            loadError = e.toUserMessage()
        }
    }

    // El aviso desaparece solo
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2500)
            toast = null
        }
    }

    val startEdit = {
        fName = current.name
        fLast = current.lastName
        fEmail = email
        fPhone = phone
        formError = null
        editing = true
    }

    val save = {
        val n = fName.trim()
        val l = fLast.trim()
        val em = fEmail.trim()
        val ph = fPhone.trim()
        when {
            n.isBlank() || l.isBlank() -> formError = "Escribe tu nombre y tu apellido."
            !EMAIL_REGEX.matches(em) ->
                formError = "El correo electrónico no es válido. Debe tener algo antes y después del @."
            ph.isBlank() -> formError = "Escribe tu número de teléfono."
            else -> {
                formError = null
                saving = true
                scope.launch {
                    try {
                        // El back devuelve un token nuevo (por si cambió el correo) y el nombre actualizado
                        val saved = ApiClient.api.updateClient(ClientUpdate(n, l, em, ph))
                        current = saved
                        // El correo y el teléfono no vienen en la respuesta:
                        // si el back aceptó el cambio, son los que se acaban de enviar
                        email = em
                        phone = ph
                        Session.user = saved          // guarda el token nuevo; el resto de la app ve el nombre nuevo
                        editing = false
                        toast = "Cambios guardados correctamente"
                    } catch (ex: CancellationException) {
                        throw ex
                    } catch (ex: Exception) {
                        formError = ex.toUserMessage()
                    }
                    saving = false
                }
            }
        }
        Unit
    }

    TerraScaffold(
        role = "Cliente",
        name = "${current.name} ${current.lastName}",
        onLogout = onLogout
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = padding.calculateTopPadding())
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 40.dp)
            ) {
                BackLink("Volver", onBack)
                Spacer(Modifier.height(12.dp))
                PageHeader("Mi cuenta", "Tu perfil", titleSize = 30.sp)
                Spacer(Modifier.height(20.dp))

                if (!editing) {
                    // ---------- Ver ----------
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(Terra, Burgundy))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    current.name.trim().firstOrNull()?.uppercase() ?: "?",
                                    color = Color.White,
                                    fontFamily = Playfair,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    "${current.name} ${current.lastName}",
                                    fontFamily = Playfair,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 22.sp,
                                    color = Charcoal
                                )
                                Text(
                                    if (loaded) email else (loadError ?: "Cargando..."),
                                    fontSize = 13.sp,
                                    color = if (loadError != null) Burgundy else Charcoal.copy(alpha = 0.6f)
                                )
                            }
                        }
                        if (loaded) {
                            Spacer(Modifier.height(16.dp))
                            InfoLine("Teléfono", phone)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    TerraButton(
                        "Mis cupones",
                        onClick = onOpenCoupons,
                        style = ButtonStyle.Outline,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    TerraButton(
                        "Editar perfil",
                        onClick = startEdit,
                        enabled = loaded,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    TerraButton(
                        "Cerrar sesión",
                        onClick = onLogout,
                        style = ButtonStyle.Outline,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(28.dp))
                    val danger = RoundedCornerShape(24.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(danger)
                            .background(Burgundy.copy(alpha = 0.05f))
                            .border(1.dp, Burgundy.copy(alpha = 0.18f), danger)
                            .padding(20.dp)
                    ) {
                        Text(
                            "Eliminar cuenta",
                            fontFamily = Playfair,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = Burgundy
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Esta acción es permanente y no se puede deshacer.",
                            fontSize = 13.sp,
                            color = Charcoal.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(14.dp))
                        TerraButton(
                            "Eliminar mi cuenta",
                            onClick = { confirmDelete = true },
                            style = ButtonStyle.Danger,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // ---------- Editar ----------
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            TerraTextField("Nombre", fName, { fName = it }, Modifier.fillMaxWidth())
                            TerraTextField("Apellido", fLast, { fLast = it }, Modifier.fillMaxWidth())
                            TerraTextField(
                                "Correo electrónico", fEmail, { fEmail = it }, Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )
                            TerraTextField(
                                "Teléfono", fPhone, { fPhone = it }, Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )
                        }
                        formError?.let {
                            Spacer(Modifier.height(12.dp))
                            Text(it, color = Burgundy, fontSize = 13.sp)
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    TerraButton(
                        if (saving) "Guardando..." else "Guardar cambios",
                        onClick = { if (!saving) save() },
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    TerraButton(
                        "Cambiar contraseña",
                        onClick = { showPassword = true },
                        style = ButtonStyle.Outline,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    TerraButton(
                        "Cancelar",
                        onClick = { editing = false; formError = null },
                        style = ButtonStyle.Outline,
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            toast?.let {
                ToastPill(
                    title = "Listo",
                    text = it,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)
                )
            }
        }
    }

    if (showPassword) {
        PasswordDialog(
            onClose = { showPassword = false },
            onSaved = {
                showPassword = false
                toast = "Contraseña actualizada"
            }
        )
    }

    if (confirmDelete) {
        TerraDialog(
            title = "Eliminar cuenta",
            message = "¿Seguro que quieres eliminar tu cuenta? Esta acción no se puede deshacer.",
            confirmLabel = "Eliminar mi cuenta",
            cancelLabel = "Cancelar",
            danger = true,
            onConfirm = {
                confirmDelete = false
                scope.launch {
                    try {
                        ApiClient.api.deleteClient().close()
                        onLogout()   // limpia la sesión y vuelve al login
                    } catch (ex: CancellationException) {
                        throw ex
                    } catch (ex: Exception) {
                        deleteError = ex.toUserMessage()
                    }
                }
            },
            onCancel = { confirmDelete = false }
        )
    }

    deleteError?.let {
        TerraDialog(
            title = "No se pudo eliminar la cuenta",
            message = it,
            confirmLabel = "Aceptar",
            onConfirm = { deleteError = null }
        )
    }
}

// Cambio de contraseña: pide la actual y la nueva dos veces (mínimo 6 caracteres, como la web)



// Cambio de contraseña: pide la actual y la nueva dos veces (mínimo 6 caracteres, como la web)
@Composable
private fun PasswordDialog(onClose: () -> Unit, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    var cur by remember { mutableStateOf("") }
    var nw by remember { mutableStateOf("") }
    var conf by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val mismatch = conf.isNotEmpty() && nw != conf
    val canSubmit = cur.isNotEmpty() && nw.length >= 6 && nw == conf && !saving
    val password = KeyboardOptions(keyboardType = KeyboardType.Password)

    AlertDialog(
        onDismissRequest = { if (!saving) onClose() },
        containerColor = Cream,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                "Cambiar contraseña",
                fontFamily = Playfair,
                fontWeight = FontWeight.SemiBold,
                color = Charcoal
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TerraTextField(
                    "Contraseña actual", cur, { cur = it }, Modifier.fillMaxWidth(),
                    keyboardOptions = password, visualTransformation = PasswordVisualTransformation()
                )
                TerraTextField(
                    "Nueva contraseña", nw, { nw = it }, Modifier.fillMaxWidth(),
                    keyboardOptions = password, visualTransformation = PasswordVisualTransformation()
                )
                TerraTextField(
                    "Confirmar nueva contraseña", conf, { conf = it }, Modifier.fillMaxWidth(),
                    keyboardOptions = password, visualTransformation = PasswordVisualTransformation()
                )
                when {
                    error != null -> Text(error ?: "", color = Burgundy, fontSize = 13.sp)
                    mismatch -> Text("Las contraseñas no coinciden.", color = Burgundy, fontSize = 13.sp)
                    else -> Text("Mínimo 6 caracteres.", color = TextSoft, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSubmit,
                onClick = {
                    saving = true
                    error = null
                    scope.launch {
                        try {
                            ApiClient.api.changePassword( ChangePasswordRequest(cur, nw)).close()
                            onSaved()
                        } catch (ex: CancellationException) {
                            throw ex
                        } catch (ex: Exception) {
                            error = ex.toUserMessage()
                        }
                        saving = false
                    }
                }
            ) {
                Text(if (saving) "Guardando..." else "Guardar", color = Terra, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onClose) {
                Text("Cancelar", color = Charcoal.copy(alpha = 0.7f))
            }
        }
    )
}