package com.example.terragalegamovile.ui

import com.example.terragalegamovile.ui.components.ButtonStack

import com.example.terragalegamovile.data.t

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.terragalegamovile.data.AppNotification
import com.example.terragalegamovile.data.Format
import com.example.terragalegamovile.data.LoginResponse
import com.example.terragalegamovile.data.NotificationStore
import com.example.terragalegamovile.ui.components.BackLink
import com.example.terragalegamovile.ui.components.ButtonStyle
import com.example.terragalegamovile.ui.components.GlassCard
import com.example.terragalegamovile.ui.components.PageHeader
import com.example.terragalegamovile.ui.components.TerraButton
import com.example.terragalegamovile.ui.components.TerraScaffold
import com.example.terragalegamovile.ui.theme.Charcoal
import com.example.terragalegamovile.ui.theme.Terra
import com.example.terragalegamovile.ui.theme.TextLabel
import com.example.terragalegamovile.ui.theme.TextSoft

// Bandeja de notificaciones del cliente (las guarda el back)
@Composable
fun NotificationsScreen(
    user: LoginResponse,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onOpen: (AppNotification) -> Unit
) {
    TerraScaffold(
        role = if (user.role == "WAITER") "Mesero" else "Cliente",
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
            BackLink(t("Volver"), onBack)
            Spacer(Modifier.height(6.dp))
            PageHeader(t("Tu bandeja"), t("Notificaciones"), titleSize = 30.sp)
            Spacer(Modifier.height(20.dp))

            if (NotificationStore.unread > 0) {
                ButtonStack {
                    TerraButton(
                        t("Marcar todas como leídas"),
                        onClick = { NotificationStore.markAllRead() },
                        style = ButtonStyle.Outline,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            if (NotificationStore.items.isEmpty()) {
                Text(t("No tienes notificaciones."), color = TextSoft)
            }

            NotificationStore.items.forEach { n ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth().clickable { onOpen(n) },
                    contentPadding = 16.dp
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (n.read) Color.Transparent else Terra)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(n.title, fontWeight = FontWeight.SemiBold, color = Charcoal)
                            Text(n.message, fontSize = 14.sp, color = TextSoft)
                            Text(Format.dateTime(n.createdAt), fontSize = 11.sp, color = TextLabel)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}