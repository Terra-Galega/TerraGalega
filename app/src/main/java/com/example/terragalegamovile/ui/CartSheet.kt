package com.example.terragalegamovile.ui

import com.example.terragalegamovile.ui.theme.TerraText

import com.example.terragalegamovile.ui.theme.Paper

import com.example.terragalegamovile.ui.components.ButtonStack

import com.example.terragalegamovile.data.t

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.terragalegamovile.data.CartStore
import com.example.terragalegamovile.data.Format
import com.example.terragalegamovile.data.removeUnavailable
import com.example.terragalegamovile.data.unavailableMessage
import com.example.terragalegamovile.ui.components.ButtonStyle
import com.example.terragalegamovile.ui.components.ProductImage
import com.example.terragalegamovile.ui.components.QuantityStepper
import com.example.terragalegamovile.ui.components.TerraButton
import com.example.terragalegamovile.ui.components.TrashIcon
import com.example.terragalegamovile.ui.theme.Charcoal
import com.example.terragalegamovile.ui.theme.Cream
import com.example.terragalegamovile.ui.theme.Playfair
import com.example.terragalegamovile.ui.theme.Terra

// Carrito compartido con la web: sube desde abajo, como en el celular de la web
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartSheet(onClose: () -> Unit, onCheckout: () -> Unit, onGoMenu: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var notice by remember { mutableStateOf<String?>(null) }

    // Al abrir (y si la web desactiva algo mientras está abierto) se quitan los productos no disponibles
    LaunchedEffect(CartStore.items) {
        val removed = CartStore.removeUnavailable()
        if (removed.isNotEmpty()) notice = unavailableMessage(removed)
    }

    val items = CartStore.items

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Cream,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    t("Mi Carrito"),
                    fontFamily = Playfair,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 24.sp,
                    color = Charcoal
                )
                Text(
                    "✕",
                    fontSize = 18.sp,
                    color = Charcoal.copy(alpha = 0.4f),
                    modifier = Modifier.clickable(onClick = onClose).padding(8.dp)
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Charcoal.copy(alpha = 0.1f)))

            Column(
                modifier = Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                notice?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Terra.copy(alpha = 0.10f))
                            .padding(12.dp)
                    ) { Text(it, color = TerraText, fontSize = 14.sp) }
                    Spacer(Modifier.height(16.dp))
                }

                if (items.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            t("Tu carrito está vacío"),
                            color = Charcoal.copy(alpha = 0.75f),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        TerraButton(t("Ver la carta"), onClick = onGoMenu, style = ButtonStyle.Outline)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Paper)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProductImage(
                                    item.product.imageUrl,
                                    Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.product.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Charcoal,
                                        maxLines = 1
                                    )
                                    if (item.addOns.isNotEmpty()) {
                                        Text(
                                            Format.group(item.addOns.map { t(it.name) })
                                                .joinToString("  ") { "x${it.second} ${it.first}" },
                                            fontSize = 12.sp,
                                            color = Charcoal.copy(alpha = 0.5f),
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        Format.money(CartStore.unitPrice(item)),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Terra
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    QuantityStepper(
                                        value = item.quantity,
                                        onChange = { CartStore.setQuantity(item, it) },
                                        compact = true
                                    )
                                }
                                TrashIcon(
                                    color = Charcoal.copy(alpha = 0.35f),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(50))
                                        .clickable { CartStore.remove(item) }
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (items.isNotEmpty()) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Charcoal.copy(alpha = 0.1f)))
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(t("Total del pedido"), fontWeight = FontWeight.Medium, color = Charcoal)
                        Text(
                            Format.money(CartStore.subtotal),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Terra
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    ButtonStack {
                        TerraButton(t("Confirmar pedido"), onClick = onCheckout, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        t("Al confirmar el pedido, te llevaremos a la pasarela de pago."),
                        fontSize = 12.sp,
                        color = Charcoal.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}