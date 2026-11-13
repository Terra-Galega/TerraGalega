package com.example.terragalegamovile.ui

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
    onTrack: () -> Unit
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
        if (ClientUi.tab == 0) {
            MenuTab(padding = padding, onOpenProduct = onOpenProduct, onOpenCategory = onOpenCategory)        } else {
            OrdersTab(
                user = user,
                padding = padding,
                onTrack = onTrack,
                onGoMenu = { ClientUi.tab = 0 }
            )
        }
    }
}