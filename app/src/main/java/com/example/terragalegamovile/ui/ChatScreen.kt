package com.example.terragalegamovile.ui

import com.example.terragalegamovile.ui.theme.Paper

import com.example.terragalegamovile.ui.theme.OkText

import com.example.terragalegamovile.ui.theme.ErrorText

import com.example.terragalegamovile.ui.theme.ErrorSurface

import com.example.terragalegamovile.ui.theme.SoftSurface

import com.example.terragalegamovile.data.t

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.terragalegamovile.R
import com.example.terragalegamovile.data.ApiClient
import com.example.terragalegamovile.data.CartStore
import com.example.terragalegamovile.data.ChatMessage
import com.example.terragalegamovile.data.ChatRequest
import com.example.terragalegamovile.data.ChatReservation
import com.example.terragalegamovile.data.apiError
import com.example.terragalegamovile.ui.components.ChatIcon
import com.example.terragalegamovile.ui.theme.Charcoal
import com.example.terragalegamovile.ui.theme.Terra
import com.example.terragalegamovile.ui.theme.tab
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val MAX_LENGTH = 500

// Colores del widget de la web (chat-widget.component.scss)
private val ChatSoft: Color get() = SoftSurface
private val ChatLine = Color(0x33B2571F)
private val ChatMuted: Color get() = Charcoal.copy(alpha = 0.6f)
private val ChatErrorBg: Color get() = ErrorSurface
private val ChatErrorText: Color get() = ErrorText
private val ChatOk: Color get() = OkText
private val ChatGlow = Color(0x59B2571F)

private val SUGGESTIONS = listOf(
    "Recomiéndame un plato",
    "Tengo antojo de algo vegetariano",
    "Quiero hacer una reserva",
    "¿Qué tengo en el carrito?",
)

// Mensaje como se pinta en el chat. error = no se pudo enviar o es un error
// (no se manda en el historial)
data class ChatBubble(
    val role: String,
    val text: String,
    val reservation: ChatReservation? = null,
    val error: Boolean = false,
)

// Conversación del cliente y si el panel está abierto. Vive mientras la app está
// abierta (como en la web) y se borra al cambiar de cuenta.
object ChatStore {
    var clientId by mutableStateOf<Int?>(null)
        private set
    var bubbles by mutableStateOf<List<ChatBubble>>(emptyList())
    var sending by mutableStateOf(false)
    var open by mutableStateOf(false)
    // Galleguito (asistente) o Soporte (chat con el restaurante); la pestañita cambia
    var mode by mutableStateOf(ChatMode.BOT)

    fun bind(id: Int?) {
        if (id != clientId) {
            clientId = id
            bubbles = emptyList()
            sending = false
            open = false
            mode = ChatMode.BOT
        }
    }
}

enum class ChatMode { BOT, SUPPORT }

// Galleguito, el pulpo de la web. Su imagen viene con fondo negro y una máscara
// aparte (así está en galleguito.svg): aquí se recorta con la máscara.
@Composable
fun Galleguito(modifier: Modifier = Modifier) {
    val color = ImageBitmap.imageResource(R.drawable.galleguito_color)
    val mask = ImageBitmap.imageResource(R.drawable.galleguito_mask)
    val toAlpha = remember {
        // Luminancia de la máscara -> transparencia (igual que el filtro del SVG)
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    0f, 0f, 0f, 0f, 0f,
                    0f, 0f, 0f, 0f, 0f,
                    0f, 0f, 0f, 0f, 0f,
                    0.2126f, 0.7152f, 0.0722f, 0f, 0f
                )
            )
        )
    }
    Canvas(modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        // Cabe completo y centrado (contain)
        val scale = minOf(size.width / color.width, size.height / color.height)
        val w = (color.width * scale).toInt()
        val h = (color.height * scale).toInt()
        val dst = IntOffset(((size.width - w) / 2).toInt(), ((size.height - h) / 2).toInt())
        drawImage(color, dstOffset = dst, dstSize = IntSize(w, h))
        drawImage(mask, dstOffset = dst, dstSize = IntSize(w, h), colorFilter = toAlpha, blendMode = BlendMode.DstIn)
    }
}

// Panel del asistente sobre la pantalla (el fondo se desenfoca en AppNav).
// Ocupa casi todo el alto; arriba tiene una pestañita con el ícono de soporte.
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BoxScope.ChatSheet(clientId: Int, firstName: String, onClose: () -> Unit) {
    var draft by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(clientId) { ChatStore.bind(clientId); ChatStore.open = true }
    BackHandler(onBack = onClose)

    // Baja al último mensaje cuando llega algo nuevo
    LaunchedEffect(ChatStore.bubbles.size, ChatStore.sending) {
        val count = 1 + ChatStore.bubbles.size + (if (ChatStore.sending) 1 else 0)
        listState.animateScrollToItem(count - 1)
    }

    fun send(text: String) {
        val message = text.trim().take(MAX_LENGTH)
        if (message.isEmpty() || ChatStore.sending) return

        // Historial para el back: solo lo que sí se envió y respondió bien
        val history = ChatStore.bubbles.filter { !it.error }.map { ChatMessage(it.role, it.text) }
        val user = ChatBubble("user", message)
        ChatStore.bubbles = ChatStore.bubbles + user
        draft = ""
        ChatStore.sending = true

        scope.launch {
            try {
                val res = ApiClient.chatApi.chat(ChatRequest(message, history))
                if (ChatStore.clientId != clientId) return@launch
                ChatStore.bubbles = ChatStore.bubbles + ChatBubble("model", res.reply, res.reservation)
                if (res.cartChanged) CartStore.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (ChatStore.clientId != clientId) return@launch
                val msg = e.apiError()?.message ?: t("No pude conectarme con el asistente. Intenta de nuevo.")
                // El mensaje del cliente queda marcado para no mandarlo en el historial
                ChatStore.bubbles = ChatStore.bubbles.map { if (it === user) it.copy(error = true) else it } +
                    ChatBubble("model", msg, error = true)
            } finally {
                ChatStore.sending = false
            }
        }
    }

    // Esquina izquierda recta: ahí se une la pestañita, como una carpeta
    val panel = RoundedCornerShape(topStart = 0.dp, topEnd = 24.dp)

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .imePadding()
    ) {
        // Pestañita hacia arriba (separador), pegada a la izquierda. Cambia de chat:
        // con Galleguito muestra el ícono de soporte; en soporte muestra a Galleguito.
        val inBot = ChatStore.mode == ChatMode.BOT
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(y = 1.dp)
                .width(76.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(color= tab)
                .clickable { ChatStore.mode = if (inBot) ChatMode.SUPPORT else ChatMode.BOT }
                .semantics {
                    contentDescription = if (inBot) t("Chatear con el restaurante") else t("Volver con Galleguito")
                }
        ) {
            if (inBot) {
                HeadsetIcon(color = Color.White, modifier = Modifier.size(20.dp))
                if (SupportStore.hasUnread) {
                    UnreadDot(Modifier.align(Alignment.TopEnd).offset(x = (-14).dp, y = 4.dp))
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Galleguito(Modifier.size(20.dp))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .shadow(24.dp, panel, ambientColor = ChatGlow, spotColor = ChatGlow)
                .clip(panel)
                .background(Paper)
                .border(1.dp, ChatGlow, panel)
                .navigationBarsPadding()
        ) {
            if (ChatStore.mode == ChatMode.SUPPORT) {
                SupportHeader(onClose)
                SupportChatContent()
            } else {
            // Encabezado terracota con Galleguito, como en la web
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Terra)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                ) {
                    Galleguito(Modifier.size(34.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        t("Galleguito - Tu asistente de TerraGalega"),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        lineHeight = 18.sp
                    )
                    Text(t("Recomendaciones, carrito y reservas"), color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                }
                if (ChatStore.bubbles.isNotEmpty()) {
                    HeaderButton(t("Nueva conversación"), enabled = !ChatStore.sending, onClick = { ChatStore.bubbles = emptyList() }) {
                        RefreshIcon()
                    }
                }
                HeaderButton(t("Cerrar asistente"), onClick = onClose) { CloseIcon() }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column {
                        Bubble(
                            ChatBubble(
                                "model",
                                t("¡Hola{}! Soy Galleguito, el asistente de ", if (firstName.isNotBlank()) ", $firstName" else "") +
                                    t("TerraGalega. Puedo recomendarte platos, agregarlos a tu carrito o hacerte una reserva.")
                            )
                        )
                        if (ChatStore.bubbles.isEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            SUGGESTIONS.forEach { s ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .padding(bottom = 6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, ChatLine, RoundedCornerShape(12.dp))
                                        .clickable(enabled = !ChatStore.sending) { send(t(s)) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(t(s), color = Terra, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    Text("›", color = Terra.copy(alpha = 0.6f), fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
                itemsIndexed(ChatStore.bubbles) { _, bubble ->
                    Column {
                        Bubble(bubble)
                        bubble.reservation?.let { r ->
                            Spacer(Modifier.height(8.dp))
                            ReservationCard(r) { url -> openStripe(context, url) }
                        }
                    }
                }
                if (ChatStore.sending) {
                    item { TypingBubble() }
                }
            }

            // Campo para escribir y botón de enviar (como el formulario de la web)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ChatLine)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(MAX_LENGTH) },
                    placeholder = { Text(t("Escribe tu mensaje..."), color = ChatMuted) },
                    shape = RoundedCornerShape(50),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ChatSoft,
                        unfocusedContainerColor = ChatSoft,
                        focusedBorderColor = Terra,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Charcoal,
                        unfocusedTextColor = Charcoal,
                        cursorColor = Terra
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                val canSend = draft.isNotBlank() && !ChatStore.sending
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (canSend) Terra else Terra.copy(alpha = 0.4f))
                        .clickable(enabled = canSend) { send(draft) }
                        .semantics { contentDescription = t("Enviar mensaje") }
                ) {
                    SendIcon()
                }
            }
            }
        }
    }
}

// Encabezado del chat con el restaurante (como el panel del operador de la web)
@Composable
private fun SupportHeader(onClose: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Terra)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
                .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
        ) {
            HeadsetIcon(color = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(t("Chat con el restaurante"), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(t("Un operador te responde sobre tu pedido"), color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
        }
        HeaderButton(t("Cerrar chat"), onClick = onClose) { CloseIcon() }
    }
}

@Composable
private fun HeaderButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label }
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
    ) { icon() }
}

@Composable
private fun Bubble(bubble: ChatBubble) {
    val mine = bubble.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Text(
            text = if (mine) AnnotatedString(bubble.text) else format(bubble.text),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = when {
                mine -> Color.White
                bubble.error -> ChatErrorText
                else -> Charcoal
            },
            modifier = Modifier
                .widthIn(max = 300.dp)
                .graphicsLayer { alpha = if (mine && bubble.error) 0.55f else 1f }
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (mine) 16.dp else 5.dp,
                        bottomEnd = if (mine) 5.dp else 16.dp
                    )
                )
                .background(
                    when {
                        mine -> Terra
                        bubble.error -> ChatErrorBg
                        else -> ChatSoft
                    }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

// "Escribiendo": tres puntitos que suben y bajan
@Composable
private fun TypingBubble() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 5.dp))
            .background(ChatSoft)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { contentDescription = t("El asistente está escribiendo") }
    ) {
        repeat(3) { i ->
            val y by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(500, delayMillis = i * 150), RepeatMode.Reverse),
                label = "dot$i"
            )
            Box(
                Modifier
                    .offset(y = (-4 * y).dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(ChatMuted)
            )
        }
    }
}

// Tarjeta de la reserva que creó o consultó el asistente (con el botón de pagar el anticipo)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ReservationCard(r: ChatReservation, onPay: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clip(RoundedCornerShape(14.dp))
            .background(Paper)
            .border(1.dp, ChatLine, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(r.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Charcoal)
        Text("${longDate(r.date)} - ${r.time.take(5)}", fontSize = 13.sp, color = ChatMuted)
        Text(
            "${r.seats} ${if (r.seats == 1) "persona" else "personas"} - Mesa ${r.tableId} (piso ${r.floor})",
            fontSize = 13.sp,
            color = ChatMuted
        )
        when {
            r.paymentState == "PENDING" && !r.checkoutUrl.isNullOrBlank() -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Terra)
                        .clickable { r.checkoutUrl?.let(onPay) }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(t("Pagar anticipo {}", money(r.depositAmount)), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    t("Tienes 30 minutos para pagarlo; si no, la reserva se cancela sola."),
                    fontSize = 11.sp,
                    color = ChatMuted,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            r.paymentState == "PAID" -> Text(
                t("✓ Anticipo pagado, reserva confirmada"),
                color = ChatOk,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// ---------- Íconos (los mismos trazos de la web) ----------

@Composable
private fun CloseIcon() {
    Canvas(Modifier.size(18.dp)) {
        val sw = size.width * 0.11f
        drawLine(Color.White, Offset(size.width * 0.2f, size.height * 0.2f), Offset(size.width * 0.8f, size.height * 0.8f), sw, StrokeCap.Round)
        drawLine(Color.White, Offset(size.width * 0.8f, size.height * 0.2f), Offset(size.width * 0.2f, size.height * 0.8f), sw, StrokeCap.Round)
    }
}

@Composable
private fun RefreshIcon() {
    Canvas(Modifier.size(18.dp)) {
        val w = size.width
        val stroke = Stroke(width = w * 0.11f, cap = StrokeCap.Round)
        drawArc(Color.White, startAngle = -60f, sweepAngle = 300f, useCenter = false,
            topLeft = Offset(w * 0.15f, w * 0.15f), size = androidx.compose.ui.geometry.Size(w * 0.7f, w * 0.7f), style = stroke)
        val head = Path().apply {
            moveTo(w * 0.62f, w * 0.10f)
            lineTo(w * 0.78f, w * 0.22f)
            lineTo(w * 0.60f, w * 0.34f)
        }
        drawPath(head, Color.White, style = stroke)
    }
}

@Composable
private fun SendIcon() {
    Canvas(Modifier.size(20.dp)) {
        val w = size.width
        val stroke = Stroke(width = w * 0.1f, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(w * 0.2f, w * 0.5f), Offset(w * 0.8f, w * 0.5f), w * 0.1f, StrokeCap.Round)
        val head = Path().apply {
            moveTo(w * 0.55f, w * 0.25f)
            lineTo(w * 0.8f, w * 0.5f)
            lineTo(w * 0.55f, w * 0.75f)
        }
        drawPath(head, Color.White, style = stroke)
    }
}

// Texto del bot: **negrita**, viñetas con "- " o "* " y saltos de línea
private fun format(text: String): AnnotatedString = buildAnnotatedString {
    val lines = text.lines().map { line -> Regex("^\\s*[-*]\\s+").replace(line, "• ") }
    val bold = Regex("\\*\\*(.+?)\\*\\*")
    lines.forEachIndexed { index, line ->
        var rest = line
        while (true) {
            val m = bold.find(rest) ?: break
            append(rest.substring(0, m.range.first))
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(m.groupValues[1]) }
            rest = rest.substring(m.range.last + 1)
        }
        append(rest)
        if (index < lines.lastIndex) append("\n")
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun longDate(date: String): String = try {
    LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "CO")))
        .replaceFirstChar { it.uppercase() }
} catch (_: Exception) {
    date
}

private fun money(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply { maximumFractionDigits = 0 }.format(value)

// Fondo oscuro detrás del panel: tocarlo cierra el chat
@Composable
fun ChatScrim(onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose
            )
    )
}
