package com.example.terragalegamovile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.terragalegamovile.data.*
import com.example.terragalegamovile.ui.components.*
import com.example.terragalegamovile.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// Después de crear la cuenta con Google: pide lo que Google no da y el
// registro normal sí exige (apellido, teléfono y una dirección de entrega).
// Al guardar, Session.user cambia a profileComplete = true y la navegación
// muestra el inicio del cliente.
@Composable
fun CompleteProfileScreen(user: LoginResponse, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(user.name) }
    var lastName by remember { mutableStateOf(user.lastName) }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    // false si la cuenta ya tenía una dirección (entonces no se pide)
    var needsAddress by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            // Gson deja null si la cuenta de Google aún no tiene teléfono
            @Suppress("USELESS_CAST")
            phone = (ApiClient.api.getClient().phone as String?) ?: ""
            needsAddress = ApiClient.api.addresses().isEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Si falla se piden todos los campos; el back decide qué hace falta
        }
    }

    fun save() {
        val n = name.trim()
        val l = lastName.trim()
        val p = phone.trim()
        val a = address.trim()
        when {
            n.isEmpty() || l.isEmpty() || p.isEmpty() || (needsAddress && a.isEmpty()) ->
                error = "Completa todos los campos para continuar."
            !Regex("^\\+?[\\d\\s-]{7,}$").matches(p) ->
                error = "Escribe un número de teléfono válido."
            else -> {
                saving = true
                error = null
                scope.launch {
                    try {
                        val updated = ApiClient.api.completeProfile(
                            CompleteProfileRequest(n, l, p, if (needsAddress) NewClientAddress(a) else null)
                        )
                        // El back devuelve un token nuevo con los mismos permisos
                        Session.user = updated
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        error = e.toUserMessage()
                    } finally {
                        saving = false
                    }
                }
            }
        }
    }

    TerraScaffold(role = "Cliente", name = "${user.name} ${user.lastName}", onLogout = onLogout) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(top = padding.calculateTopPadding())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 40.dp)
        ) {
            PageHeader("Ya casi está", "Completa tu perfil")
            Text(
                "Creaste tu cuenta con Google. Para poder entregarte tus pedidos necesitamos algunos datos más.",
                fontSize = 15.sp,
                color = Charcoal.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
            )

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                TerraTextField(label = "Nombre", value = name, onValueChange = { name = it.take(70) })
                Spacer(Modifier.height(16.dp))
                TerraTextField(label = "Apellido", value = lastName, onValueChange = { lastName = it.take(70) })
                Spacer(Modifier.height(16.dp))
                TerraTextField(
                    label = "Teléfono",
                    value = phone,
                    onValueChange = { phone = it.take(25) },
                    placeholder = "3001234567",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                if (needsAddress) {
                    Spacer(Modifier.height(16.dp))
                    TerraTextField(
                        label = "Dirección de entrega",
                        value = address,
                        onValueChange = { address = it.take(CheckoutDraft.ADDRESS_MAX) },
                        placeholder = "Calle 8 Sur #68C-20"
                    )
                }

                error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = Burgundy, fontSize = 14.sp)
                }

                Spacer(Modifier.height(24.dp))
                TerraButton(
                    text = if (saving) "Guardando..." else "Guardar y continuar",
                    onClick = { save() },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Cerrar sesión",
                    color = Charcoal.copy(alpha = 0.65f),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable(enabled = !saving, onClick = onLogout)
                        .padding(8.dp)
                )
            }
        }
    }
}
