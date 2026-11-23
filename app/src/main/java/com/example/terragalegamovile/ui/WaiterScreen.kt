package com.example.terragalegamovile.ui

import com.example.terragalegamovile.ui.theme.Gold

import androidx.compose.ui.hapticfeedback.HapticFeedbackType

import androidx.compose.ui.platform.LocalHapticFeedback

import com.example.terragalegamovile.ui.components.RefreshBox

import com.example.terragalegamovile.ui.theme.ForestText

import com.example.terragalegamovile.ui.theme.TerraText

import com.example.terragalegamovile.ui.theme.ErrorText

import com.example.terragalegamovile.ui.theme.Paper

import com.example.terragalegamovile.ui.components.ButtonStack

import com.example.terragalegamovile.data.t

import com.example.terragalegamovile.ui.components.ErrorNotice

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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
    "PENDING" -> t("En espera")
    "PREPARING" -> t("En preparación")
    else -> t("Listo")
}

private fun itemColor(status: String) = when (status) {
    "PENDING" -> Charcoal.copy(alpha = 0.6f)
    "PREPARING" -> Terra
    else -> Available
}

// La bandeja pide abrir una mesa (al tocar un aviso de cocina)
object WaiterUi {
    var openTable by mutableStateOf<Int?>(null)
    // Pedido al que se refiere el aviso: la mesa se desplaza hasta él y lo resalta
    var focusOrder by mutableStateOf<Int?>(null)
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
            initialInfo = tables.firstOrNull { it.id == table },
            onBack = {
                selectedTable = null
                WaiterUi.focusOrder = null
            }
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
    // Filtro "Reservadas hoy"
    var onlyReserved by remember { mutableStateOf(false) }
    // Deslizar hacia abajo para actualizar
    var reload by remember { mutableStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }

    // Recarga el piso cada pocos segundos mientras la pantalla esté abierta
    LaunchedEffect(floor, reload) {
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
            refreshing = false
            delay(REFRESH_MS)
        }
    }

    TerraScaffold(role = "Mesero", name = "${user.name} ${user.lastName}", onLogout = onLogout) { padding ->
        RefreshBox(isRefreshing = refreshing, onRefresh = { refreshing = true; reload++ }, topPadding = padding.calculateTopPadding()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = padding.calculateTopPadding())
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 40.dp)
            ) {
                PageHeader(t("Portal del mesero"), t("Mesas"))
                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillChip(t("Piso 1"), floor == 1) { onFloor(1) }
                    PillChip(t("Piso 2"), floor == 2) { onFloor(2) }
                    // Solo las mesas del piso con reservas hoy, con su lista de horas
                    PillChip(t("Reservadas hoy"), onlyReserved) { onlyReserved = !onlyReserved }
                }
                Spacer(Modifier.height(16.dp))

                error?.let { ErrorNotice(it, Modifier.padding(bottom = 12.dp), onRetry = { error = null; reload++ }) }

                val shown = tables.filter { it.floor == floor }
                if (loading && shown.isEmpty()) {
                    Text(t("Cargando mesas..."), color = TextSoft, fontSize = 14.sp)
                } else if (onlyReserved) {
                    ReservedToday(shown, onOpen)
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
}

// Mesas del piso con reservas hoy: hora, a nombre de quién, personas y estado
@Composable
private fun ReservedToday(tables: List<WaiterTable>, onOpen: (Int) -> Unit) {
    val reserved = tables.filter { it.todayReservations.isNotEmpty() }
        .sortedBy { tbl -> tbl.todayReservations.minOf { it.time } }
    if (reserved.isEmpty()) {
        Text(t("No hay mesas reservadas hoy en este piso."), color = TextSoft, fontSize = 14.sp)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        reserved.forEach { table ->
            val takenByOther = table.occupied && !table.mine
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Paper.copy(alpha = 0.9f))
                    .border(1.dp, Gold.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                    .clickable(enabled = !takenByOther) { onOpen(table.id) }
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t("Mesa {}", table.id), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Charcoal, modifier = Modifier.weight(1f))
                    Text(
                        when {
                            takenByOther -> t("La atiende otro mesero")
                            table.occupied -> t("Ocupada")
                            else -> t("Libre")
                        },
                        fontSize = 12.sp,
                        color = Charcoal.copy(alpha = 0.6f)
                    )
                }
                table.todayReservations.sortedBy { it.time }.forEach { r ->
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            hhmm(r.time),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TerraText,
                            modifier = Modifier.width(56.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(r.clientName ?: r.name ?: "-", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Charcoal)
                            Text(
                                listOfNotNull(
                                    r.seats?.let { if (it == 1) t("1 persona") else t("{} personas", it) },
                                    r.name?.takeIf { it.isNotBlank() && it != r.clientName }?.let { "$it" }
                                ).joinToString(" · "),
                                fontSize = 12.sp,
                                color = Charcoal.copy(alpha = 0.6f)
                            )
                        }
                        Text(
                            when {
                                r.seated -> t("Ya llegaron")
                                r.paid -> t("Anticipo pagado")
                                else -> t("Sin pagar")
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                r.seated -> ForestText
                                r.paid -> ForestText
                                else -> ErrorText
                            }
                        )
                    }
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
        takenByOther -> t.attendingWaiterName?.let { t("Atiende {}", it) } ?: t("La atiende otro mesero")
        t.readyToServe -> t("¡Para servir!")
        t.activeOrders > 0 -> if (t.activeOrders == 1) t("En cocina") else t("{} en cocina", t.activeOrders)
        // Ya comió: falta registrar el pago para liberar la mesa
        t.awaitingPayment > 0 -> t("Por cobrar")
        else -> t("Sin pedidos")
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
                if (takenByOther) stateDescription = t("Deshabilitada, la atiende otro mesero")
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
            if (t.occupied) t("Ocupada") else t("Libre"),
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
                t("Reservada · {}", hhmm(it)),
                fontSize = 12.sp,
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
                t("⚠ Reserva {}", hhmm(it)),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = ErrorText,
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
    initialInfo: WaiterTable?,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    // Datos de la mesa (reserva, quién la atiende...). Se recargan junto con los
    // pedidos: así el aviso de reserva no se queda con la foto de cuando se entró
    var infoState by remember { mutableStateOf(initialInfo) }
    val info = infoState
    // Error al recargar: se muestra en la pantalla (no una ventana cada pocos segundos)
    var loadError by remember { mutableStateOf<String?>(null) }
    // Otro mesero tomó la mesa: se avisa una vez y se vuelve a las mesas
    var lostAccess by remember { mutableStateOf<String?>(null) }
    // Pregunta "¿son los de la reserva?" antes de tomar el primer pedido
    var askReservation by remember { mutableStateOf(false) }
    var forReservation by rememberSaveable { mutableStateOf<Boolean?>(null) }
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
                val floor = info?.floor ?: (tableId / 100)
                ApiClient.api.waiterTables(floor).firstOrNull { it.id == tableId }?.let { infoState = it }
                loadError = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if ((e as? retrofit2.HttpException)?.code() == 403) {
                    lostAccess = e.toUserMessage()
                    break
                }
                loadError = e.toUserMessage()
            }
            loading = false
            delay(REFRESH_MS)
        }
    }

    // Borrador guardado de esta mesa (si se cerró la app o venció la sesión)
    val savedDraft = remember(newOrder, tableId) { AppStorage.loadWaiterDraft(user.id, tableId) }

    fun startOrder() {
        when {
            // Hay un pedido sin enviar: se sigue con él (ya se respondió la pregunta)
            savedDraft != null -> {
                forReservation = savedDraft.forReservation
                newOrder = true
            }
            // Mesa libre con una reserva que importa: primero se pregunta
            orders.isEmpty() && info?.reservationId != null -> askReservation = true
            else -> {
                forReservation = null
                newOrder = true
            }
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
                dialog = t("No se pudo completar") to e.toUserMessage()
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
            forReservation = forReservation,
            onCancel = { newOrder = false },
            onSent = {
                newOrder = false
                reload++
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                dialog = t("Pedido enviado a cocina") to t("La cocina ya lo ve en su pantalla.")
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
            WaiterBackButton(t("Volver a las mesas"), onClick = onBack)
            Spacer(Modifier.height(8.dp))
            PageHeader(
                if (info != null) t("Piso {} · {} sillas", info.floor, info.seats) else t("Mesa"),
                t("Mesa {}", tableId)
            )

            loadError?.let {
                Spacer(Modifier.height(10.dp))
                Text(
                    t("No pudimos actualizar la mesa. {}", it),
                    fontSize = 13.sp,
                    color = ErrorText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Burgundy.copy(alpha = 0.07f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            // Reserva que importa al sentar a alguien (solo con la mesa libre)
            if (info != null && info.reservationId != null && orders.isEmpty()) {
                val who = info.reservationName?.let { "$it" } ?: t("La reserva")
                val time = hhmm(info.reservationTime)
                val unpaid = if (info.reservationPaid) "" else t(" (anticipo aún sin pagar)")
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background((if (info.walkInAllowed) Gold else Burgundy).copy(alpha = 0.1f))
                        .border(1.dp, (if (info.walkInAllowed) Gold else Burgundy).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        if (info.reservationNoShow) t("{} no ha llegado ({})", who, time) else t("Reservada · {}", time),
                        fontWeight = FontWeight.SemiBold,
                        color = ErrorText,
                        fontSize = 15.sp
                    )
                    Text(
                        when {
                            info.reservationNoShow -> t("Pasó el tiempo de espera: ya puedes dar la mesa a otros clientes. Si llegan, confírmalo al tomar el pedido.")
                            else -> t("{}{}. Solo puedes sentar aquí a los de la reserva.", who, unpaid)
                        },
                        fontSize = 14.sp,
                        color = Charcoal.copy(alpha = 0.75f)
                    )
                }
            }

            info?.upcomingReservation?.takeIf { info.reservationId == null || orders.isNotEmpty() }?.let { time ->
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Gold.copy(alpha = 0.15f))
                        .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Text(t("⚠ Reserva a las {}", hhmm(time)), fontWeight = FontWeight.SemiBold, color = ErrorText, fontSize = 15.sp)
                    Text(
                        (info.upcomingReservationName?.let { "$it. " } ?: "") +
                            t("Ten en cuenta que esta mesa debe quedar libre para entonces."),
                        fontSize = 14.sp,
                        color = Charcoal.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            ButtonStack {
                TerraButton(
                    if (savedDraft != null) t("Seguir pedido sin enviar ({})", savedDraft.lines.sumOf { it.quantity }) else t("Nuevo pedido"),
                    onClick = { startOrder() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
                    Text(t("Cobrar mesa"), fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Charcoal)
                    Text(
                        t("{} pedidos entregados por cobrar. Se cobran todos juntos.", awaiting.size),
                        fontSize = 14.sp,
                        color = Charcoal.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TerraButton(
                            t("Efectivo"),
                            onClick = { toPayTable = "CASH" },
                            enabled = !busy,
                            style = ButtonStyle.Sage,
                            modifier = Modifier.weight(1f)
                        )
                        TerraButton(
                            t("Tarjeta"),
                            onClick = { toPayTable = "CARD" },
                            enabled = !busy,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            when {
                loading -> Text(t("Cargando pedidos..."), color = TextSoft, fontSize = 14.sp)
                orders.isEmpty() -> Text(
                    t("Mesa libre: no tiene pedidos en curso ni por cobrar."),
                    color = TextSoft,
                    fontSize = 15.sp
                )
                else -> orders.forEachIndexed { index, order ->
                    // Si el aviso era de este pedido: se desplaza hasta él y se resalta unos segundos
                    val requester = remember(order.id) { BringIntoViewRequester() }
                    val focused = WaiterUi.focusOrder == order.id
                    if (focused) {
                        LaunchedEffect(order.id) {
                            delay(300)
                            requester.bringIntoView()
                            delay(4_000)
                            if (WaiterUi.focusOrder == order.id) WaiterUi.focusOrder = null
                        }
                    }
                    OrderCard(
                        modifier = Modifier.bringIntoViewRequester(requester),
                        highlight = focused,
                        order = order,
                        round = if (orders.size > 1) index + 1 else null,
                        busy = busy,
                        onDeliver = {
                            act({ ApiClient.api.deliverTableOrder(order.id) }, t("Pedido entregado"))
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
            title = t("Cobrar mesa {}", tableId),
            message = t("¿La mesa pagó en {}? Se cobran los {} pedidos entregados.", label, count) +
                if (stillCooking > 0) t("\n\nOjo: aún hay pedidos en cocina; se cobran cuando se entreguen.")
                else t("\n\nLa mesa quedará libre."),
            confirmLabel = t("Sí, cobrar en {}", label),
            cancelLabel = t("Volver"),
            onConfirm = {
                toPayTable = null
                act({ ApiClient.api.payTable(tableId, PayRequest(method)) }, t("Mesa cobrada en {}", label))
            },
            onCancel = { toPayTable = null }
        )
    }

    toPay?.let { (order, method) ->
        val label = if (method == "CASH") "efectivo" else "tarjeta"
        TerraDialog(
            title = t("Cobrar pedido #{}", order.id),
            message = t("¿La mesa pagó en {}? Al confirmarlo el pedido queda cobrado", label) +
                t(" y, si no le queda nada más, la mesa vuelve a estar libre."),
            confirmLabel = t("Sí, cobrar en {}", label),
            cancelLabel = t("Volver"),
            onConfirm = {
                toPay = null
                act({ ApiClient.api.payTableOrder(order.id, PayRequest(method)) }, t("Pedido cobrado en {}", label))
            },
            onCancel = { toPay = null }
        )
    }

    toCancel?.let { order ->
        TerraDialog(
            title = t("Cancelar pedido #{}", order.id),
            message = t("¿Seguro? Solo se puede porque la cocina aún no lo empezó."),
            confirmLabel = t("Sí, cancelar"),
            cancelLabel = t("No"),
            danger = true,
            onConfirm = {
                toCancel = null
                act({ ApiClient.api.cancelTableOrder(order.id) }, t("Pedido cancelado"))
            },
            onCancel = { toCancel = null }
        )
    }
    if (askReservation && info != null) {
        val time = hhmm(info.reservationTime)
        TerraDialog(
            title = t("¿Son los de la reserva?"),
            message = t(
                "La mesa {} tiene una reserva a las {} a nombre de {}.",
                tableId, time, info.reservationClientName ?: info.reservationName ?: "-"
            ) +
                (info.reservationName?.takeIf { it.isNotBlank() && it != info.reservationClientName }
                    ?.let { " " + t("Reserva: {}.", it) } ?: "") +
                if (info.walkInAllowed) "" else t(" Si no son ellos, sienta a estos clientes en otra mesa."),
            confirmLabel = t("Sí, son ellos"),
            onConfirm = {
                askReservation = false
                forReservation = true
                newOrder = true
            },
            cancelLabel = if (info.walkInAllowed) t("No, otros clientes") else t("No, otra mesa"),
            onCancel = {
                askReservation = false
                if (info.walkInAllowed) {
                    forReservation = false
                    newOrder = true
                }
            }
        )
    }
    lostAccess?.let { message ->
        TerraDialog(t("Ya no puedes ver esta mesa"), message, t("Volver a las mesas"), onConfirm = {
            lostAccess = null
            onBack()
        })
    }
    dialog?.let { (title, message) ->
        TerraDialog(title, message.ifBlank { " " }, t("Aceptar"), onConfirm = { dialog = null })
    }
}

@Composable
private fun OrderCard(
    order: TableOrder,
    round: Int?,
    busy: Boolean,
    onDeliver: () -> Unit,
    onCancel: () -> Unit,
    onPay: (String) -> Unit,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .then(if (highlight) Modifier.border(2.dp, Terra, RoundedCornerShape(24.dp)) else Modifier),
        contentPadding = 18.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    t("Pedido #{}", order.id) + (round?.let { t(" · ronda {}", it) } ?: ""),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Charcoal
                )
                Text("${clock(order.startTime)} · ${order.waiterName}", fontSize = 13.sp, color = TextSoft)
            }
            StatusPill(
                when {
                    order.awaitingPayment -> t("Entregado · por cobrar")
                    order.ready -> t("Listo en cocina")
                    order.status == "PENDING" -> t("En espera")
                    else -> t("En preparación")
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
                        Text("+ ${item.addOns.joinToString(", ")}", fontSize = 13.sp, color = ForestText)
                    }
                    item.notes?.let { Text("“$it”", fontSize = 13.sp, color = TerraText) }
                }
                Text(itemLabel(item.status), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = itemColor(item.status))
            }
        }
        Spacer(Modifier.height(12.dp))
        when {
            // Entregado: el mesero registra cómo pagó la mesa
            order.awaitingPayment -> Column {
                Text(t("Método de pago"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Charcoal)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TerraButton(
                        t("Efectivo"),
                        onClick = { onPay("CASH") },
                        enabled = !busy,
                        style = ButtonStyle.Sage,
                        modifier = Modifier.weight(1f)
                    )
                    TerraButton(
                        t("Tarjeta"),
                        onClick = { onPay("CARD") },
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            order.ready -> ButtonStack {
                TerraButton(
                    if (busy) t("Entregando...") else t("Entregar en la mesa"),
                    onClick = onDeliver,
                    enabled = !busy,
                    style = ButtonStyle.Sage,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            order.cancellable -> ButtonStack {
                TerraButton(
                    t("Cancelar pedido"),
                    onClick = onCancel,
                    enabled = !busy,
                    style = ButtonStyle.Danger,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> Text(
                t("La cocina ya lo está preparando: no se puede cancelar."),
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
    forReservation: Boolean?,
    onCancel: () -> Unit,
    onSent: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    // El borrador se recupera del teléfono: sobrevive a cerrar la app, a perder
    // la red y a que venza la sesión
    val saved = remember(tableId) { AppStorage.loadWaiterDraft(user.id, tableId) }
    val draft = remember(tableId) {
        mutableStateListOf<DraftLine>().apply {
            saved?.lines?.forEach { add(DraftLine(it.product, it.quantity, it.notes, it.addOns)) }
        }
    }
    // Id del pedido: el mismo en todos los reintentos, así cocina no lo recibe dos veces
    val requestId = remember(tableId) { saved?.requestId ?: java.util.UUID.randomUUID().toString() }
    val reservationChoice = saved?.forReservation ?: forReservation

    // Guarda el borrador en cada cambio
    LaunchedEffect(tableId) {
        snapshotFlow { draft.toList() }.collect { lines ->
            AppStorage.saveWaiterDraft(
                user.id,
                tableId,
                WaiterDraft(requestId, reservationChoice, lines.map { WaiterDraftLine(it.product, it.quantity, it.notes, it.addOns) })
            )
        }
    }
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
            error = t("Agrega al menos un plato.")
            return
        }
        sending = true
        error = null
        scope.launch {
            try {
                ApiClient.api.createTableOrder(
                    NewTableOrder(
                        clientRequestId = requestId,
                        forReservation = reservationChoice,
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
                // Ya está en cocina: se borra el borrador
                AppStorage.saveWaiterDraft(user.id, tableId, null)
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
                WaiterBackButton(t("Volver a la mesa {}", tableId), onClick = onCancel)
                Spacer(Modifier.height(8.dp))
                PageHeader(t("Mesa {}", tableId), t("Nuevo pedido"))
                Spacer(Modifier.height(16.dp))
                TerraTextField(label = t("Buscar plato"), value = search, onValueChange = { search = it }, placeholder = t("Ej: pulpo"))
                Spacer(Modifier.height(20.dp))
                if (products.isEmpty() && error == null) {
                    Text(t("Cargando la carta..."), color = TextSoft, fontSize = 14.sp)
                }
            }

            // Una fila por categoría, en el orden de la carta
            val shown = products.filter { it.active && it.name.contains(search.trim(), ignoreCase = true) }
            shown.groupBy { it.category?.name ?: t("Otros") }.forEach { (category, items) ->
                SectionLabel(t(category), modifier = Modifier.padding(horizontal = 20.dp))
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
                    Text(t("Tu pedido"), fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Charcoal)
                    Spacer(Modifier.height(8.dp))
                    if (draft.isEmpty()) {
                        Text(t("Toca un plato de la carta para agregarlo."), fontSize = 14.sp, color = TextSoft)
                    }
                    draft.forEachIndexed { index, line ->
                        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 6.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${line.quantity}× ${line.product.name}", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Charcoal)
                                if (line.addOns.isNotEmpty()) {
                                    Text("+ ${line.addOns.joinToString(", ") { t(it.name) }}", fontSize = 13.sp, color = ForestText)
                                }
                                if (line.notes.isNotBlank()) {
                                    Text("“${line.notes.trim()}”", fontSize = 13.sp, color = TerraText)
                                }
                            }
                            Text(
                                t("Quitar"),
                                color = ErrorText,
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
                    ErrorNotice(it, Modifier.padding(vertical = 10.dp))
                }

                val count = draft.sumOf { it.quantity }
                Spacer(Modifier.height(14.dp))
                ButtonStack {
                    TerraButton(
                        when {
                            sending -> t("Enviando...")
                            count == 0 -> t("Agrega platos al pedido")
                            else -> t("Enviar a cocina ({} {})", count, if (count == 1) t("plato") else t("platos"))
                        },
                        onClick = { send() },
                        enabled = !sending && count > 0,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    TerraButton(t("Cancelar"), onClick = onCancel, style = ButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
                }
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
            .background(Paper.copy(alpha = 0.9f))
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
                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TerraText)
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
                    Text(t("Cantidad"), fontSize = 15.sp, color = Charcoal, modifier = Modifier.weight(1f))
                    QuantityStepper(value = quantity, onChange = { quantity = it }, min = 1, max = 99, compact = true)
                }
                Spacer(Modifier.height(14.dp))
                Text(t("Adicionales"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Charcoal)
                Spacer(Modifier.height(4.dp))
                when {
                    options == null -> Text(t("Cargando..."), fontSize = 13.sp, color = TextSoft)
                    options!!.isEmpty() -> Text(t("Este plato no tiene adicionales."), fontSize = 13.sp, color = TextSoft)
                    else -> options!!.forEach { addOn ->
                        CheckRow(
                            label = "${t(addOn.name)} (+${Format.money(addOn.price)})",
                            checked = chosen.any { it.id == addOn.id },
                            accent = Terra
                        ) {
                            if (chosen.any { it.id == addOn.id }) chosen.removeAll { it.id == addOn.id } else chosen.add(addOn)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                TerraTextField(
                    label = t("Nota para cocina (opcional)"),
                    value = notes,
                    onValueChange = { notes = it.take(200) },
                    placeholder = t("Ej: sin cebolla, término medio")
                )
            }
        },
        confirmButton = {
            TerraButton(
                t("Agregar al pedido"),
                onClick = { onAdd(DraftLine(product, quantity, notes, chosen.toList())) },
                compact = true
            )
        },
        dismissButton = {
            TerraButton(t("Cancelar"), onClick = onDismiss, style = ButtonStyle.Outline, compact = true)
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
        Text(text, color = TerraText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}
