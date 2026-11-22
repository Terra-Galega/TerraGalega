package com.example.terragalegamovile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.terragalegamovile.data.ApiClient
import com.example.terragalegamovile.data.AppNotification
import com.example.terragalegamovile.data.ClientCoupon
import com.example.terragalegamovile.data.Format
import com.example.terragalegamovile.data.LoginResponse
import com.example.terragalegamovile.data.NotificationStore
import com.example.terragalegamovile.data.toUserMessage
import com.example.terragalegamovile.ui.components.BackLink
import com.example.terragalegamovile.ui.components.ButtonStyle
import com.example.terragalegamovile.ui.components.GlassCard
import com.example.terragalegamovile.ui.components.PageHeader
import com.example.terragalegamovile.ui.components.TerraButton
import com.example.terragalegamovile.ui.components.TerraScaffold
import com.example.terragalegamovile.ui.theme.Burgundy
import com.example.terragalegamovile.ui.theme.Charcoal
import com.example.terragalegamovile.ui.theme.Forest
import com.example.terragalegamovile.ui.theme.Playfair
import com.example.terragalegamovile.ui.theme.Terra
import com.example.terragalegamovile.ui.theme.TextLabel
import com.example.terragalegamovile.ui.theme.TextSoft
import kotlin.coroutines.cancellation.CancellationException

// Mis cupones: activos arriba, usados o vencidos abajo (igual que la web)
@Composable
fun CouponsScreen(user: LoginResponse, onBack: () -> Unit, onLogout: () -> Unit) {
    var coupons by remember { mutableStateOf<List<ClientCoupon>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            coupons = ApiClient.api.myCoupons()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.toUserMessage()
        }
        loaded = true
    }

    val active = coupons.filter { it.status == "ACTIVE" }
    val expired = coupons.filter { it.status != "ACTIVE" }

    TerraScaffold(
        role = "Cliente",
        name = "${user.name} ${user.lastName}",
        onLogout = onLogout
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = padding.calculateTopPadding())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 40.dp)
        ) {
            BackLink("Volver", onBack)
            Spacer(Modifier.height(6.dp))
            PageHeader("Mi cuenta", "Mis cupones", titleSize = 30.sp)
            Spacer(Modifier.height(20.dp))

            when {
                !loaded -> Text("Cargando...", color = TextSoft)
                error != null -> Text(error ?: "", color = Burgundy)
                coupons.isEmpty() -> Text("Aún no tienes cupones.", color = TextSoft)
            }

            if (active.isNotEmpty()) {
                Text("Activos", fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Charcoal)
                Spacer(Modifier.height(10.dp))
                active.forEach { CouponCard(it) }
            }
            if (expired.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Expirados", fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Charcoal)
                Spacer(Modifier.height(10.dp))
                expired.forEach { CouponCard(it) }
            }
        }
    }
}

@Composable
private fun CouponCard(c: ClientCoupon) {
    val isActive = c.status == "ACTIVE"
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.code, fontWeight = FontWeight.Bold, color = if (isActive) Terra else TextSoft, modifier = Modifier.weight(1f))
            Text(
                when (c.status) {
                    "ACTIVE" -> Format.couponValue(c.discountType, c.discountValue)
                    "USED" -> "Usado"
                    "EXHAUSTED" -> "Agotado"
                    else -> "Vencido"
                },
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) Forest else TextSoft
            )
        }
        c.description?.let { Text(it, fontSize = 14.sp, color = TextSoft) }
        c.minOrderAmount?.let {
            Text("Pedido mínimo ${Format.money(it)}", fontSize = 12.sp, color = TextLabel)
        }
        c.expiresAt?.let {
            Text("Vence: ${Format.dateTime(it)}", fontSize = 12.sp, color = TextLabel)
        }
    }
    Spacer(Modifier.height(10.dp))
}




















































































































































































































































































































































































