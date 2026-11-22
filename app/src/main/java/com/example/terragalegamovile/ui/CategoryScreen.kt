package com.example.terragalegamovile.ui

import com.example.terragalegamovile.data.t

import com.example.terragalegamovile.ui.components.ErrorNotice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.terragalegamovile.data.ApiClient
import com.example.terragalegamovile.data.CartStore
import com.example.terragalegamovile.data.LoginResponse
import com.example.terragalegamovile.data.Product
import com.example.terragalegamovile.data.toUserMessage
import com.example.terragalegamovile.ui.components.BackLink
import com.example.terragalegamovile.ui.components.FeatureFilterButton
import com.example.terragalegamovile.ui.components.PageHeader
import com.example.terragalegamovile.ui.components.ProductCard
import com.example.terragalegamovile.ui.components.TerraScaffold
import com.example.terragalegamovile.ui.components.featureLabels
import com.example.terragalegamovile.ui.components.terraFieldColors
import com.example.terragalegamovile.ui.theme.Burgundy
import com.example.terragalegamovile.ui.theme.Charcoal
import com.example.terragalegamovile.ui.theme.Terra
import com.example.terragalegamovile.ui.theme.TextSoft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import com.example.terragalegamovile.data.ClientUi
import com.example.terragalegamovile.ui.components.ClientBottomBar

// Platos de una categoría ("Todos" y "Popular" también), con buscador y filtro de características
@Composable
fun CategoryScreen(
    user: LoginResponse,
    category: String,
    onBack: () -> Unit,
    onOpenProduct: (Int) -> Unit,
    onOpenCart: () -> Unit,
    onLogout: () -> Unit
) {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var features by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                products = ApiClient.api.products()
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!loaded) error = e.toUserMessage()
            }
            loaded = true
            delay(10_000)
        }
    }

    val term = search.trim().lowercase()
    val inCategory = when (category) {
        "Todos" -> products
        "Popular" -> products.filter { it.popular }
        else -> products.filter { it.category?.name == category }
    }
    val filtered = inCategory.filter { p ->
        val labels = p.featureLabels()
        p.name.lowercase().contains(term) && features.all { it in labels }
    }

    TerraScaffold(
        role = "Cliente",
        name = "${user.name} ${user.lastName}",
        onLogout = onLogout,
        cartCount = CartStore.count,
        onCart = onOpenCart,
        bottomBar = { haze ->
            ClientBottomBar(
                hazeState = haze,
                selected = 0,
                cartCount = CartStore.count,
                onMenu = onBack,
                onCart = onOpenCart,
                onOrders = { ClientUi.tab = 1; onBack() },
                onLogout = onLogout
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding() + 24.dp            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { BackLink(t("Todas las categorías"), onBack) }

            item {
                PageHeader(
                    eyebrow = t("Nuestra Carta"),
                    title = if (category == "Todos") t("Todos los platos") else t(category)
                )
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        singleLine = true,
                        placeholder = { Text(t("Buscar platos…"), color = Charcoal.copy(alpha = 0.4f)) },
                        shape = RoundedCornerShape(50),
                        colors = terraFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    FeatureFilterButton(active = features) { features = it }
                }
            }

            when {
                !loaded -> item { Text(t("Cargando platos..."), color = TextSoft) }

                error != null && products.isEmpty() -> item { ErrorNotice(error ?: "") }

                filtered.isEmpty() -> item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(t("No se encontraron platos"), color = Charcoal.copy(alpha = 0.35f), fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            t("Quitar filtros"),
                            color = Terra,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { search = ""; features = emptySet() }
                        )
                    }
                }

                else -> items(filtered, key = { it.id }) { product ->
                    ProductCard(product = product, onClick = { onOpenProduct(product.id) })
                }
            }
        }
    }
}