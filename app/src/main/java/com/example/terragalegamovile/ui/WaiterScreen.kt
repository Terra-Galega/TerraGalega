package com.example.terragalegamovile.ui

import android.R
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.terragalegamovile.data.*
import com.example.terragalegamovile.ui.components.*
import com.example.terragalegamovile.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Cada cuánto se refrescan las mesas y los pedidos de una mesa (como el resto
// de la app: consultas periódicas mientras la pantalla está abierta)
private const val REFRESH_MS = 6_000L

// "19:30:00" -> "19:30"
private fun hhmm(time: String?): String = time?.take(5) ?: ""

// "2026-11-19T09:42:10.123" -> "09:42"
private fun clock(dateTime: String): String = dateTime.substringAfter('T').take(5)

private fun itemLabel(status: String) = when (status) {
    "PENDING" -> "En espera"
    "PREPARING" -> "En preparación"
    else -> "Listo"
}

private fun itemColor(status: String) = when (status) {
    "PENDING" -> Charcoal.copy(alpha = 0.6f)
    "PREPARING" -> Terra
    else -> Available
}

// La bandeja pide abrir una mesa (al tocar un aviso de cocina)
object WaiterUi {
    var openTable by mutableStateOf<Int?>(null)
}

// Plato elegido en un pedido nuevo (aún no enviado). El mismo plato con
// adicionales o notas distintos va en líneas separadas.
private data class DraftLine(
    val product: Product,
    val quantity: Int,
    val notes: String,
    val addOns: List<AddOnOption>
)

// Portal del mesero: elige el piso, ve sus mesas (con el aviso de reserva
// próxima), entra a una mesa para ver sus pedidos y registra pedidos nuevos.
@Composable
fun WaiterScreen(user: LoginResponse, onLogout: () -> Unit) {
    var floor by rememberSaveable { mutableIntStateOf(1) }
    var selectedTable by rememberSaveable { mutableStateOf<Int?>(null) }
    // Última foto de las mesas del piso (la pantalla de la mesa usa su aviso de reserva)
    var tables by remember { mutableStateOf<List<WaiterTable>>(emptyList()) }

    // Al tocar un aviso de cocina en la bandeja se abre esa mesa
    LaunchedEffect(WaiterUi.openTable) {
        WaiterUi.openTable?.let {
            selectedTable = it
            WaiterUi.openTable = null
        }
    }

    val table = selectedTable
    if (table == null) {
        FloorView(
            user = user,
            onLogout = onLogout,
            floor = floor,
            onFloor = { floor = it },
            tables = tables,
            onTables = { tables = it },
            onOpen = { selectedTable = it }
        )
    } else {
        TableView(
            user = user,
            onLogout = onLogout,
            tableId = table,
            info = tables.firstOrNull { it.id == table },
            onBack = { selectedTable = null }
        )
    }
}

// ---------- Pisos y mesas ----------

@Composable
private fun FloorView(
    user: LoginResponse,
    onLogout: () -> Unit,
    floor: Int,
    onFloor: (Int) -> Unit,
    tables: List<WaiterTable>,
    onTables: (List<WaiterTable>) -> Unit,
    onOpen: (Int) -> Unit
) {
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember(floor) { mutableStateOf(true) }

    // Recarga el piso cada pocos segundos mientras la pantalla esté abierta
    LaunchedEffect(floor) {
        while (isActive) {
            try {
                onTables(ApiClient.api.waiterTables(floor))
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.toUserMessage()
            }
            loading = false
            delay(REFRESH_MS)
        }
    }

    TerraScaffold(role = "Mesero", name = "${user.name} ${user.lastName}", onLogout = onLogout) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = padding.calculateTopPadding())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 40.dp)
        ) {
            PageHeader("Portal del mesero", "Mesas")
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillChip("Piso 1", floor == 1) { onFloor(1) }
                PillChip("Piso 2", floor == 2) { onFloor(2) }
            }
            Spacer(Modifier.height(16.dp))

            error?.let { Text(it, color = Burgundy, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp)) }

            val shown = tables.filter { it.floor == floor }
            if (loading && shown.isEmpty()) {
                Text("Cargando mesas...", color = TextSoft, fontSize = 14.sp)
            } else {
                // 3 mesas por fila
                shown.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { t ->
                            TableCard(t, Modifier.weight(1f)) { onOpen(t.id) }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun TableCard(t: WaiterTable, modifier: Modifier, onClick: () -> Unit) {
    // Tres estados de la mesa:
    // - libre: verde, cualquiera la puede tomar
    // - ocupada y la atiendo yo: naranja
    // - ocupada por otro mesero: gris y deshabilitada (no se puede entrar)
    val takenByOther = t.occupied && !t.mine
    val main = when {
        takenByOther -> Charcoal.copy(alpha = 0.4f)
        t.occupied -> Terra
        else -> Forest
    }
    val label = when {
        takenByOther -> t.attendingWaiterName?.let { "Atiende $it" } ?: "La atiende otro mesero"
        t.readyToServe -> "¡Para servir!"
        t.activeOrders > 0 -> if (t.activeOrders == 1) "En cocina" else "${t.activeOrders} en cocina"
        // Ya comió: falta registrar el pago para liberar la mesa
        t.awaitingPayment > 0 -> "Por cobrar"
        else -> "Sin pedidos"
    }
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                when {
                    takenByOther -> Charcoal.copy(alpha = 0.06f)
                    t.occupied -> Terra.copy(alpha = if (t.readyToServe) 0.16f else 0.07f)
                    else -> Forest.copy(alpha = 0.07f)
                }
            )
            .border(1.5.dp, if (takenByOther) Charcoal.copy(alpha = 0.15f) else main, shape)
            .clickable(enabled = !takenByOther, onClick = onClick)
            .semantics {
                if (takenByOther) stateDescription = "Deshabilitada, la atiende otro mesero"
            }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${t.id}",
                fontFamily = Playfair,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                color = if (takenByOther) Charcoal.copy(alpha = 0.4f) else Charcoal,
                modifier = Modifier.weight(1f)
            )
            // Ocupada desde que se toma el pedido hasta que se cobra todo
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(main)
            )
        }
        Text(
            if (t.occupied) "Ocupada" else "Libre",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = main
        )
        Text(
            "${t.seats} sillas",
            fontSize = 12.sp,
            color = if (takenByOther) Charcoal.copy(alpha = 0.4f) else TextSoft
        )
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = main)
        // Aviso: reserva en 2 horas o menos
        // Reserva en curso ahora mismo: la mesa es de quien reservó
        t.currentReservation?.let {
            Spacer(Modifier.height(6.dp))
            Text(
                "Reservada · ${hhmm(it)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Burgundy)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        t.upcomingReservation?.let {
            Spacer(Modifier.height(6.dp))
            Text(
                "⚠ Reserva ${hhmm(it)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Burgundy,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Gold.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

// ---------- Una mesa: sus pedidos y el pedido nuevo ----------

@Composable
private fun TableView(
    user: LoginResponse,
    onLogout: () -> Unit,
    tableId: Int,
    info: WaiterTable?,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var orders by remember { mutableStateOf<List<TableOrder>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var newOrder by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var toCancel by remember { mutableStateOf<TableOrder?>(null) }
    // Cobro pendiente de confirmar: pedido y método ("CASH" | "CARD")
    var toPay by remember { mutableStateOf<Pair<TableOrder, String>?>(null) }
    // "Cobrar mesa" pendiente de confirmar: método ("CASH" | "CARD")
    var toPayTable by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(tableId, reload) {
        while (isActive) {
            try {
                orders = ApiClient.api.tableOrders(tableId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                dialog = "No se pudieron cargar los pedidos" to e.toUserMessage()
            }
            loading = false
            delay(REFRESH_MS)
        }
    }

    fun act(call: suspend () -> Any, done: String) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                call()
                dialog = done to ""
                reload++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                dialog = "No se pudo completar" to e.toUserMessage()
                reload++
            } finally {
                busy = false
            }
        }
    }

    if (newOrder) {
        NewOrderView(
            user = user,
            onLogout = onLogout,
            tableId = tableId,
            onCancel = { newOrder = false },
            onSent = {
                newOrder = false
                reload++
                dialog = "Pedido enviado a cocina" to "La cocina ya lo ve en su pantalla."
            }
        )
        return
    }

    TerraScaffold(role = "Mesero", name = "${user.name} ${user.lastName}", onLogout = onLogout) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = padding.calculateTopPadding())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 40.dp)
        ) {
            // El gesto/botón "atrás" del teléfono también vuelve a las mesas
            BackHandler(onBack = onBack)
            WaiterBackButton("Volver a las mesas", onClick = onBack)
            Spacer(Modifier.height(8.dp))
            PageHeader(
                if (info != null) "Piso ${info.floor} · ${info.seats} sillas" else "Mesa",
                "Mesa $tableId"
            )

            info?.upcomingReservation?.let { time ->
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Gold.copy(alpha = 0.15f))
                        .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Text("⚠ Reserva a las ${hhmm(time)}", fontWeight = FontWeight.SemiBold, color = Burgundy, fontSize = 15.sp)
                    Text(
                        (info.upcomingReservationName?.let { "«$it». " } ?: "") +
                            "Ten en cuenta que esta mesa debe quedar libre para entonces.",
                        fontSize = 14.sp,
                        color = Charcoal.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            TerraButton("Nuevo pedido", onClick = { newOrder = true }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))

            // Cobrar mesa: con 2 o más pedidos por cobrar, se cobran todos de una vez
            val awaiting = orders.filter { it.awaitingPayment }
            if (awaiting.size >= 2) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Gold.copy(alpha = 0.12f))
                        .border(1.dp, Gold.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Text("Cobrar mesa", fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Charcoal)
                    Text(
                        "${awaiting.size} pedidos entregados por cobrar. Se cobran todos juntos.",
                        fontSize = 14.sp,
                        color = Charcoal.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TerraButton(
                            "Efectivo",
                            onClick = { toPayTable = "CASH" },
                            enabled = !busy,
                            style = ButtonStyle.Sage,
                            modifier = Modifier.weight(1f)
                        )
                        TerraButton(
                            "Tarjeta",
                            onClick = { toPayTable = "CARD" },
                            enabled = !busy,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            when {
                loading -> Text("Cargando pedidos...", color = TextSoft, fontSize = 14.sp)
                orders.isEmpty() -> Text(
                    "Mesa libre: no tiene pedidos en curso ni por cobrar.",
                    color = TextSoft,
                    fontSize = 15.sp
                )
                else -> orders.forEachIndexed { index, order ->
                    OrderCard(
                        order = order,
                        round = if (orders.size > 1) index + 1 else null,
                        busy = busy,
                        onDeliver = {
                            act({ ApiClient.api.deliverTableOrder(order.id) }, "Pedido entregado")
                        },
                        onCancel = { toCancel = order },
                        onPay = { method -> toPay = order to method }
                    )
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }

    toPayTable?.let { method ->
        val label = if (method == "CASH") "efectivo" else "tarjeta"
        val count = orders.count { it.awaitingPayment }
        val stillCooking = orders.count { it.status == "PENDING" || it.status == "IN_PROGRESS" }
        TerraDialog(
            title = "Cobrar mesa $tableId",
            message = "¿La mesa pagó en $label? Se cobran los $count pedidos entregados." +
                if (stillCooking > 0) "\n\nOjo: aún hay pedidos en cocina; se cobran cuando se entreguen."
                else "\n\nLa mesa quedará libre.",
            confirmLabel = "Sí, cobrar en $label",
            cancelLabel = "Volver",
            onConfirm = {
                toPayTable = null
                act({ ApiClient.api.payTable(tableId, PayRequest(method)) }, "Mesa cobrada en $label")
            },
            onCancel = { toPayTable = null }
        )
    }

    toPay?.let { (order, method) ->
        val label = if (method == "CASH") "efectivo" else "tarjeta"
        TerraDialog(
            title = "Cobrar pedido #${order.id}",
            message = "¿La mesa pagó en $label? Al confirmarlo el pedido queda cobrado" +
                " y, si no le queda nada más, la mesa vuelve a estar libre.",
            confirmLabel = "Sí, cobrar en $label",
            cancelLabel = "Volver",
            onConfirm = {
                toPay = null
                act({ ApiClient.api.payTableOrder(order.id, PayRequest(method)) }, "Pedido cobrado en $label")
            },
            onCancel = { toPay = null }
        )
    }

    toCancel?.let { order ->
        TerraDialog(
            title = "Cancelar pedido #${order.id}",
            message = "¿Seguro? Solo se puede porque la cocina aún no lo empezó.",
            confirmLabel = "Sí, cancelar",
            cancelLabel = "No",
            danger = true,
            onConfirm = {
                toCancel = null
                act({ ApiClient.api.cancelTableOrder(order.id) }, "Pedido cancelado")
            },
            onCancel = { toCancel = null }
        )
    }
    dialog?.let { (title, message) ->
        TerraDialog(title, message.ifBlank { " " }, "Aceptar", onConfirm = { dialog = null })
    }
}

@Composable
private fun OrderCard(
    order: TableOrder,
    round: Int?,
    busy: Boolean,
    onDeliver: () -> Unit,
    onCancel: () -> Unit,
    onPay: (String) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Pedido #${order.id}" + (round?.let { " · ronda $it" } ?: ""),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Charcoal
                )
                Text("${clock(order.startTime)} · ${order.waiterName}", fontSize = 13.sp, color = TextSoft)
            }
            StatusPill(
                when {
                    order.awaitingPayment -> "Entregado · por cobrar"
                    order.ready -> "Listo en cocina"
                    order.status == "PENDING" -> "En espera"
                    else -> "En preparación"
                },
                when {
                    order.awaitingPayment -> Gold
                    order.ready -> Forest
                    order.status == "PENDING" -> Charcoal
                    else -> Terra
                }
            )
        }
        Spacer(Modifier.height(10.dp))
        order.items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${item.quantity}× ${item.productName}", fontSize = 15.sp, color = Charcoal)
                    if (item.addOns.isNotEmpty()) {
                        Text("+ ${item.addOns.joinToString(", ")}", fontSize = 13.sp, color = Forest)
                    }
                    item.notes?.let { Text("“$it”", fontSize = 13.sp, color = Terra) }
                }
                Text(itemLabel(item.status), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = itemColor(item.status))
            }
        }
        Spacer(Modifier.height(12.dp))
        when {
            // Entregado: el mesero registra cómo pagó la mesa
            order.awaitingPayment -> Column {
                Text("Método de pago", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Charcoal)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TerraButton(
                        "Efectivo",
                        onClick = { onPay("CASH") },
                        enabled = !busy,
                        style = ButtonStyle.Sage,
                        modifier = Modifier.weight(1f)
                    )
                    TerraButton(
                        "Tarjeta",
                        onClick = { onPay("CARD") },
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            order.ready -> TerraButton(
                if (busy) "Entregando..." else "Entregar en la mesa",
                onClick = onDeliver,
                enabled = !busy,
                style = ButtonStyle.Sage,
                modifier = Modifier.fillMaxWidth()
            )
            order.cancellable -> TerraButton(
                "Cancelar pedido",
                onClick = onCancel,
                enabled = !busy,
                style = ButtonStyle.Danger,
                modifier = Modifier.fillMaxWidth()
            )
            else -> Text(
                "La cocina ya lo está preparando: no se puede cancelar.",
                fontSize = 13.sp,
                color = TextSoft
            )
        }
    }
}

// ---------- Pedido nuevo ----------

// Carta en filas: una columna con una fila deslizable (LazyRow) por categoría.
// Al tocar un plato se elige cantidad, adicionales de su categoría y nota.
// Abajo, "Tu pedido" con las líneas elegidas y el botón para enviarlo a cocina.
@Composable
private fun NewOrderView(
    user: LoginResponse,
    onLogout: () -> Unit,
    tableId: Int,
    onCancel: () -> Unit,
    onSent: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    val draft = remember { mutableStateListOf<DraftLine>() }
    // Plato que se está configurando en la ventana (null = cerrada)
    var choosing by remember { mutableStateOf<Product?>(null) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            products = ApiClient.api.products()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.toUserMessage()
        }
    }

    fun send() {
        if (sending) return
        if (draft.isEmpty()) {
            error = "Agrega al menos un plato."
            return
        }
        sending = true
        error = null
        scope.launch {
            try {
                ApiClient.api.createTableOrder(
                    NewTableOrder(
                        tableId = tableId,
                        items = draft.map { line ->
                            NewOrderItem(
                                productId = line.product.id,
                                quantity = line.quantity,
                                notes = line.notes.trim().ifBlank { null },
                                addOnIds = line.addOns.map { it.id }
                            )
                        }
                    )
                )
                onSent()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.toUserMessage()
            } finally {
                sending = false
            }
        }
    }

    TerraScaffold(role = "Mesero", name = "${user.name} ${user.lastName}", onLogout = onLogout) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(top = padding.calculateTopPadding())
                .padding(top = 16.dp, bottom = 40.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                BackHandler(onBack = onCancel)
                WaiterBackButton("Volver a la mesa $tableId", onClick = onCancel)
                Spacer(Modifier.height(8.dp))
                PageHeader("Mesa $tableId", "Nuevo pedido")
                Spacer(Modifier.height(16.dp))
                TerraTextField(label = "Buscar plato", value = search, onValueChange = { search = it }, placeholder = "Ej: pulpo")
                Spacer(Modifier.height(20.dp))
                if (products.isEmpty() && error == null) {
                    Text("Cargando la carta...", color = TextSoft, fontSize = 14.sp)
                }
            }

            // Una fila por categoría, en el orden de la carta
            val shown = products.filter { it.active && it.name.contains(search.trim(), ignoreCase = true) }
            shown.groupBy { it.category?.name ?: "Otros" }.forEach { (category, items) ->
                SectionLabel(category, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.id }) { product ->
                        DishCard(
                            product = product,
                            count = draft.filter { it.product.id == product.id }.sumOf { it.quantity },
                            onClick = { choosing = product }
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // ---------- Tu pedido ----------
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 18.dp) {
                    Text("Tu pedido", fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Charcoal)
                    Spacer(Modifier.height(8.dp))
                    if (draft.isEmpty()) {
                        Text("Toca un plato de la carta para agregarlo.", fontSize = 14.sp, color = TextSoft)
                    }
                    draft.forEachIndexed { index, line ->
                        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 6.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${line.quantity}× ${line.product.name}", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Charcoal)
                                if (line.addOns.isNotEmpty()) {
                                    Text("+ ${line.addOns.joinToString(", ") { it.name }}", fontSize = 13.sp, color = Forest)
                                }
                                if (line.notes.isNotBlank()) {
                                    Text("“${line.notes.trim()}”", fontSize = 13.sp, color = Terra)
                                }
                            }
                            Text(
                                "Quitar",
                                color = Burgundy,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { draft.removeAt(index) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                error?.let {
                    Text(it, color = Burgundy, fontSize = 14.sp, modifier = Modifier.padding(vertical = 10.dp))
                }

                val count = draft.sumOf { it.quantity }
                Spacer(Modifier.height(14.dp))
                TerraButton(
                    when {
                        sending -> "Enviando..."
                        count == 0 -> "Agrega platos al pedido"
                        else -> "Enviar a cocina ($count ${if (count == 1) "plato" else "platos"})"
                    },
                    onClick = { send() },
                    enabled = !sending && count > 0,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                TerraButton("Cancelar", onClick = onCancel, style = ButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    choosing?.let { product ->
        DishDialog(
            product = product,
            onDismiss = { choosing = null },
            onAdd = { line ->
                draft.add(line)
                choosing = null
                error = null
            }
        )
    }
}

// Tarjeta de un plato dentro de la fila de su categoría
@Composable
private fun DishCard(product: Product, count: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .width(150.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.9f))
            .border(if (count > 0) 1.5.dp else 1.dp, if (count > 0) Terra else BorderSoft, shape)
            .clickable(onClick = onClick)
    ) {
        Box {
            ProductImage(product.imageUrl, modifier = Modifier.fillMaxWidth().height(96.dp))
            // Cuántas unidades de este plato lleva ya el pedido
            if (count > 0) {
                Text(
                    "$count",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Terra)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        Column(modifier = Modifier.padding(10.dp)) {
            Text(product.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Charcoal, maxLines = 2, minLines = 2)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Format.money(product.price), fontSize = 13.sp, color = TextSoft, modifier = Modifier.weight(1f))
                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Terra)
            }
        }
    }
}

// Ventana para configurar un plato: cantidad, adicionales de su categoría y nota
@Composable
private fun DishDialog(product: Product, onDismiss: () -> Unit, onAdd: (DraftLine) -> Unit) {
    var quantity by remember { mutableIntStateOf(1) }
    var notes by remember { mutableStateOf("") }
    var options by remember { mutableStateOf<List<AddOnOption>?>(null) }
    val chosen = remember { mutableStateListOf<AddOnOption>() }

    // Adicionales activos de la categoría del plato (los mismos del pedido a domicilio)
    LaunchedEffect(product.id) {
        val categoryId = product.category?.id
        options = if (categoryId == null) emptyList() else try {
            ApiClient.api.addOns(categoryId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Cream,
        shape = RoundedCornerShape(24.dp),
        title = { Text(product.name, fontFamily = Playfair, fontWeight = FontWeight.SemiBold, color = Charcoal) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Cantidad", fontSize = 15.sp, color = Charcoal, modifier = Modifier.weight(1f))
                    QuantityStepper(value = quantity, onChange = { quantity = it }, min = 1, max = 99, compact = true)
                }
                Spacer(Modifier.height(14.dp))
                Text("Adicionales", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Charcoal)
                Spacer(Modifier.height(4.dp))
                when {
                    options == null -> Text("Cargando...", fontSize = 13.sp, color = TextSoft)
                    options!!.isEmpty() -> Text("Este plato no tiene adicionales.", fontSize = 13.sp, color = TextSoft)
                    else -> options!!.forEach { addOn ->
                        CheckRow(
                            label = "${addOn.name} (+${Format.money(addOn.price)})",
                            checked = chosen.any { it.id == addOn.id },
                            accent = Terra
                        ) {
                            if (chosen.any { it.id == addOn.id }) chosen.removeAll { it.id == addOn.id } else chosen.add(addOn)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                TerraTextField(
                    label = "Nota para cocina (opcional)",
                    value = notes,
                    onValueChange = { notes = it.take(200) },
                    placeholder = "Ej: sin cebolla, término medio"
                )
            }
        },
        confirmButton = {
            TerraButton(
                "Agregar al pedido",
                onClick = { onAdd(DraftLine(product, quantity, notes, chosen.toList())) },
                compact = true
            )
        },
        dismissButton = {
            TerraButton("Cancelar", onClick = onDismiss, style = ButtonStyle.Outline, compact = true)
        }
    )
}

// Botón para volver, grande y visible: el mesero lo usa con prisa y con una mano
@Composable
private fun WaiterBackButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(Terra.copy(alpha = 0.1f))
            .border(1.5.dp, Terra, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = text }
            .padding(start = 14.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(50))
                .background(Terra),
            contentAlignment = Alignment.Center
        ) {
            Text("←", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, color = Terra, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}
