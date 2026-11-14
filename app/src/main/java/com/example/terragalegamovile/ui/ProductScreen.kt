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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.terragalegamovile.data.AddOnOption
import com.example.terragalegamovile.data.ApiClient
import com.example.terragalegamovile.data.CartStore
import com.example.terragalegamovile.data.Format
import com.example.terragalegamovile.data.LoginResponse
import com.example.terragalegamovile.data.Product
import com.example.terragalegamovile.data.ProductDetail
import com.example.terragalegamovile.data.toProduct
import com.example.terragalegamovile.data.toUserMessage
import com.example.terragalegamovile.ui.components.AddOnRow
import com.example.terragalegamovile.ui.components.BackLink
import com.example.terragalegamovile.ui.components.FeatureChips
import com.example.terragalegamovile.ui.components.GlassCard
import com.example.terragalegamovile.ui.components.ProductCard
import com.example.terragalegamovile.ui.components.ProductImage
import com.example.terragalegamovile.ui.components.QuantityStepper
import com.example.terragalegamovile.ui.components.SectionLabel
import com.example.terragalegamovile.ui.components.TerraButton
import com.example.terragalegamovile.ui.components.TerraDialog
import com.example.terragalegamovile.ui.components.TerraScaffold
import com.example.terragalegamovile.ui.components.ToastPill
import com.example.terragalegamovile.ui.components.featureLabels
import com.example.terragalegamovile.ui.theme.Charcoal
import com.example.terragalegamovile.ui.theme.Gold
import com.example.terragalegamovile.ui.theme.Playfair
import com.example.terragalegamovile.ui.theme.Terra
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
@Composable
fun ProductScreen(
    user: LoginResponse,
    productId: Int,
    onBack: () -> Unit,
    onOpenProduct: (Int) -> Unit,
    onOpenCart: () -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var product by remember { mutableStateOf<ProductDetail?>(null) }
    var related by remember { mutableStateOf<List<Product>>(emptyList()) }
    var addOns by remember { mutableStateOf<List<AddOnOption>>(emptyList()) }
    var selected by remember { mutableStateOf<List<AddOnOption>>(emptyList()) } // una entrada por unidad
    var quantity by remember { mutableStateOf(1) }
    var verifying by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<Pair<String, String>?>(null) }
    var unavailable by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Producto y relacionados: carga y refresca cada 30 s (como la web)
    LaunchedEffect(productId) {
        while (true) {
            try {
                val p = ApiClient.api.product(productId)
                if (!p.active) {
                    unavailable = "Este producto ya no está disponible."
                    break
                }
                product = p
                try {
                    related = ApiClient.api.relatedProducts(productId)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (product == null) {
                    unavailable = if (e is HttpException) "Este producto no existe o ya no está disponible."
                    else e.toUserMessage()
                    break
                }
            }
            delay(30_000)
        }
    }

    // Adicionales activos de la categoría: refresca cada 10 s sin perder lo elegido
    val categoryId = product?.category?.id
    LaunchedEffect(categoryId) {
        if (categoryId == null) {
            addOns = emptyList()
            return@LaunchedEffect
        }
        while (true) {
            try {
                addOns = ApiClient.api.addOns(categoryId)
                selected = selected.mapNotNull { s -> addOns.find { it.id == s.id } }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
            delay(10_000)
        }
    }

    // El aviso desaparece solo
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2500)
            toast = null
        }
    }

    fun addOnQuantity(a: AddOnOption) = selected.count { it.id == a.id }

    fun changeAddOn(a: AddOnOption, newValue: Int) {
        val current = addOnQuantity(a)
        if (newValue > current) {
            selected = selected + a
        } else if (newValue < current) {
            val index = selected.indexOfFirst { it.id == a.id }
            if (index != -1) selected = selected.filterIndexed { i, _ -> i != index }
        }
    }

    // Botón "Añadir a la orden": vuelve a consultar el producto (pudo desactivarse o cambiar de precio)
    fun addToOrder() {
        val p = product ?: return
        if (verifying) return
        verifying = true
        scope.launch {
            try {
                val current = ApiClient.api.product(p.id)
                if (!current.active) {
                    unavailable = "Este producto ya no está disponible."
                    return@launch
                }
                val qty = quantity
                CartStore.add(current.toProduct(), qty, selected) { error ->
                    if (error == null) toast = "Añadido al carrito" to "${qty}× ${current.name}"
                    else errorMsg = error
                }
                selected = emptyList()
                quantity = 1
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e is HttpException) unavailable = "Este producto ya no está disponible."
                else errorMsg = "No se pudo verificar el producto. Revisa tu conexión e intenta de nuevo."
            } finally {
                verifying = false
            }
        }
    }

    TerraScaffold(
        role = "Cliente",
        name = "${user.name} ${user.lastName}",
        onLogout = onLogout,
        cartCount = CartStore.count,
        onCart = onOpenCart
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = padding.calculateTopPadding())
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 40.dp)
            ) {
                BackLink("Volver a la carta", onBack)
                Spacer(Modifier.height(16.dp))

                product?.let { p ->
                    val total = (p.price + selected.sumOf { it.price }) * quantity
                    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
                        ProductImage(
                            p.imageUrl,
                            Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(16.dp))
                        )
                        Spacer(Modifier.height(20.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            p.category?.let {
                                Text(
                                    it.name.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.8.sp,
                                    color = Terra.copy(alpha = 0.8f)
                                )
                                Spacer(Modifier.width(12.dp))
                            }
                            if (p.popular) {
                                Text("★ Popular", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            p.name,
                            fontFamily = Playfair,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 32.sp,
                            lineHeight = 38.sp,
                            color = Charcoal
                        )
                        Spacer(Modifier.height(14.dp))
                        Box(Modifier.width(48.dp).height(2.dp).background(Terra))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            p.description ?: "",
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = Charcoal.copy(alpha = 0.65f)
                        )
                        val labels = p.featureLabels()
                        if (labels.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            FeatureChips(labels)
                        }

                        p.category?.let { c ->
                            Spacer(Modifier.height(20.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Charcoal.copy(alpha = 0.04f))
                                    .border(1.dp, Charcoal.copy(alpha = 0.07f), RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    "CATEGORÍA: ${c.name.uppercase()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.8.sp,
                                    color = Terra.copy(alpha = 0.8f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(c.description ?: "", fontSize = 14.sp, color = Charcoal.copy(alpha = 0.6f))
                            }
                        }

                        if (addOns.isNotEmpty()) {
                            Spacer(Modifier.height(24.dp))
                            SectionLabel("Adicionales opcionales")
                            Spacer(Modifier.height(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                addOns.forEach { a ->
                                    AddOnRow(a, addOnQuantity(a)) { changeAddOn(a, it) }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            QuantityStepper(value = quantity, onChange = { quantity = it })
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "TOTAL",
                                    fontSize = 11.sp,
                                    letterSpacing = 1.8.sp,
                                    color = Charcoal.copy(alpha = 0.35f)
                                )
                                Text(
                                    Format.money(total),
                                    fontFamily = Playfair,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp,
                                    color = Terra
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        TerraButton(
                            text = "Añadir a la orden",
                            onClick = { addToOrder() },
                            enabled = !verifying,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (related.isNotEmpty()) {
                    Spacer(Modifier.height(40.dp))
                    Text(
                        "DE LA MISMA CATEGORÍA",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 3.sp,
                        color = Terra.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "También te puede gustar",
                        fontFamily = Playfair,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 28.sp,
                        color = Charcoal
                    )
                    Spacer(Modifier.height(20.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        items(related, key = { it.id }) { r ->
                            ProductCard(
                                product = r,
                                onClick = { onOpenProduct(r.id) },
                                modifier = Modifier.width(210.dp),
                                horizontalMargin = 0.dp,
                                imageHeight = 130.dp,
                                showCategory = false
                            )
                        }
                    }
                }
            }

            toast?.let { (title, text) ->
                ToastPill(
                    title = title,
                    text = text,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)
                )
            }
        }
    }

    unavailable?.let {
        TerraDialog(
            title = "Producto no disponible",
            message = it,
            confirmLabel = "Aceptar",
            onConfirm = { unavailable = null; onBack() }
        )
    }
    errorMsg?.let {
        TerraDialog(
            title = "No se pudo añadir al carrito",
            message = it,
            confirmLabel = "Aceptar",
            onConfirm = { errorMsg = null }
        )
    }
}