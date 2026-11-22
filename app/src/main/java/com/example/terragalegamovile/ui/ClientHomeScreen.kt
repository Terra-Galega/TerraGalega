package com.example.terragalegamovile.ui

import com.example.terragalegamovile.data.t

import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import com.example.terragalegamovile.data.CartStore
import com.example.terragalegamovile.data.ClientUi
import com.example.terragalegamovile.data.LoginResponse
import com.example.terragalegamovile.ui.components.ClientBottomBar
import com.example.terragalegamovile.ui.components.TerraScaffold

@Composable
fun ClientHomeScreen(
    user: LoginResponse,
    onLogout: () -> Unit,
    onOpenProduct: (Int) -> Unit,
    onOpenCategory: (String) -> Unit,   // <-- NUEVO
    onOpenCart: () -> Unit,
    onTrack: () -> Unit,
    onOpenChat: () -> Unit = {}
) {
    TerraScaffold(
        role = "Cliente",
        name = "${user.name} ${user.lastName}",
        onLogout = onLogout,
        cartCount = CartStore.count,
        onCart = onOpenCart,
        bottomBar = { haze ->
            ClientBottomBar(
                hazeState = haze,
                selected = ClientUi.tab,
                cartCount = CartStore.count,
                onMenu = { ClientUi.tab = 0 },
                onCart = onOpenCart,
                onOrders = { ClientUi.tab = 1 },
                onLogout = onLogout
            )
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize()) {
            if (ClientUi.tab == 0) {
                MenuTab(padding = padding, onOpenProduct = onOpenProduct, onOpenCategory = onOpenCategory)
            } else {
                OrdersTab(
                    user = user,
                    padding = padding,
                    onTrack = onTrack,
                    onGoMenu = { ClientUi.tab = 0 }
                )
            }
        }
    }
}
