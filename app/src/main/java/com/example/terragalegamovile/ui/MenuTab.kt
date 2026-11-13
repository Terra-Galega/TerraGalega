package com.example.terragalegamovile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.terragalegamovile.data.ApiClient
import com.example.terragalegamovile.data.Category
import com.example.terragalegamovile.data.Product
import com.example.terragalegamovile.data.toUserMessage
import com.example.terragalegamovile.ui.components.PageHeader
import com.example.terragalegamovile.ui.components.ProductCard
import com.example.terragalegamovile.ui.components.ProductImage
import com.example.terragalegamovile.ui.components.terraFieldColors
import com.example.terragalegamovile.ui.theme.Burgundy
import com.example.terragalegamovile.ui.theme.Charcoal
import com.example.terragalegamovile.ui.theme.Playfair
import com.example.terragalegamovile.ui.theme.Terra
import com.example.terragalegamovile.ui.theme.TextSoft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

// Pestaña "Carta": buscador y botones de categoría. Los platos se ven al entrar a una categoría.
@Composable
fun MenuTab(
    padding: PaddingValues,
    onOpenProduct: (Int) -> Unit,
    onOpenCategory: (String) -> Unit
) {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    val seed = remember { kotlin.random.Random.nextInt(1000) }

    // Carga la carta y la refresca cada 10 s (igual que la web)
    LaunchedEffect(Unit) {
        while (true) {
            try {
                products = ApiClient.api.products()
                if (categories.isEmpty()) categories = ApiClient.api.categories()
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
    val results = if (term.isEmpty()) emptyList() else products.filter { it.name.lowercase().contains(term) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = padding.calculateTopPadding() + 16.dp,
            bottom = padding.calculateBottomPadding() + 24.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            PageHeader("Lo que ofrecemos", "Nuestra Carta")
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                singleLine = true,
                placeholder = { Text("Buscar platos…", color = Charcoal.copy(alpha = 0.4f)) },
                shape = RoundedCornerShape(50),
                colors = terraFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        when {
            !loaded -> item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Cargando la carta...", color = TextSoft)
            }

            error != null && products.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                Text(error ?: "", color = Burgundy)
            }

            // Buscando: se muestran los platos que coinciden, de toda la carta
            term.isNotEmpty() -> {
                if (results.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No se encontraron platos", color = Charcoal.copy(alpha = 0.35f), fontSize = 18.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Borrar búsqueda",
                                color = Terra,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { search = "" }
                            )
                        }
                    }
                } else {
                    items(results, key = { it.id }, span = { GridItemSpan(maxLineSpan) }) { product ->
                        ProductCard(product = product, onClick = { onOpenProduct(product.id) })
                    }
                }
            }

            // Sin buscar: botones de categoría
            else -> items(listOf("Todos", "Popular") + categories.map { it.name }) { name ->
                val dishes = when (name) {
                    "Todos" -> products
                    "Popular" -> products.filter { it.popular }
                    else -> products.filter { it.category?.name == name }
                }

                CategoryTile(
                    name = name,
                    count = dishes.size,
                    imageUrl = if (name == "Todos") {
                        // Un plato al azar (solo de los que tienen foto) en vez del primero
                        val withImage = dishes.filter { !it.imageUrl.isNullOrBlank() }
                        if (withImage.isEmpty()) null else withImage[seed % withImage.size].imageUrl
                    } else {
                        dishes.firstOrNull()?.imageUrl
                    },
                    onClick = { onOpenCategory(name) }
                )
            }
        }
    }
}

// Cuadrado redondeado con la foto de un plato de la categoría y su nombre encima
@Composable
private fun CategoryTile(name: String, count: Int, imageUrl: String?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(
                10.dp, shape,
                ambientColor = Charcoal.copy(alpha = 0.12f),
                spotColor = Charcoal.copy(alpha = 0.12f)
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(Terra, Burgundy)))
            .clickable(onClick = onClick)
    ) {
        imageUrl?.let { ProductImage(it, Modifier.fillMaxSize()) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Charcoal.copy(alpha = 0.8f)))
        )
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(
                name,
                fontFamily = Playfair,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                color = Color.White
            )
            Text(
                if (count == 1) "1 plato" else "$count platos",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}