package com.example.terragalegamovile.ui

import com.example.terragalegamovile.ui.theme.TerraText

import com.example.terragalegamovile.ui.theme.Paper

import com.example.terragalegamovile.ui.theme.ErrorText

import com.example.terragalegamovile.ui.theme.SoftSurface

import com.example.terragalegamovile.data.t

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.terragalegamovile.data.ApiClient
import com.example.terragalegamovile.data.AppStorage
import com.example.terragalegamovile.data.OrderChatMessage
import com.example.terragalegamovile.data.OrderChatSummary
import com.example.terragalegamovile.data.SendChatMessage
import com.example.terragalegamovile.data.Session
import com.example.terragalegamovile.data.apiError
import com.example.terragalegamovile.ui.theme.Charcoal
import com.example.terragalegamovile.ui.theme.Terra
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

private const val SUPPORT_MAX = 500
private val SupportSoft: Color get() = SoftSurface
private val SupportLine = Color(0x33B2571F)
private val SupportMuted: Color get() = Charcoal.copy(alpha = 0.6f)
private val SupportError: Color get() = ErrorText

// Chat con el restaurante (operador) sobre un pedido, el mismo de la web:
// - qué pedidos tienen chat activo y si hay mensajes nuevos del operador (puntito),
// - qué mensajes ya se vieron (se guarda en el teléfono).
object SupportStore {
    private const val POLL_MS = 6_000L

    var summaries by mutableStateOf<List<OrderChatSummary>>(emptyList())
        private set
    // Pedido abierto en el panel (null = elegir pedido)
    var openOrderId by mutableStateOf<Int?>(null)
    private var seen by mutableStateOf<Map<Int, Int>>(emptyMap())
    private var clientId: Int? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null

    // Pedidos con mensajes del operador que el cliente no ha visto
    val unreadIds: Set<Int>
        get() = summaries.filter {
            it.lastSenderRole == "OPERATOR" && it.orderId != openOrderIfVisible() &&
                it.lastMessageId > (seen[it.orderId] ?: 0)
        }.map { it.orderId }.toSet()

    val hasUnread: Boolean get() = unreadIds.isNotEmpty()

    private fun openOrderIfVisible(): Int? =
        if (ChatStore.open && ChatStore.mode == ChatMode.SUPPORT) openOrderId else null

    fun start() {
        if (job != null) return
        job = scope.launch {
            Session.state
                .map { user -> user?.takeIf { it.role == "CLIENT" }?.id }
                .distinctUntilChanged()
                .collectLatest { id ->
                    clientId = id
                    summaries = emptyList()
                    openOrderId = null
                    seen = if (id != null) AppStorage.loadChatSeen(id) else emptyMap()
                    if (id == null) return@collectLatest
                    while (isActive) {
                        refreshNow()
                        delay(POLL_MS)
                    }
                }
        }
    }

    suspend fun refreshNow() {
        try {
            summaries = ApiClient.api.orderChatActive()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Sin conexión: se queda lo que había
        }
    }

    fun markSeen(orderId: Int, lastId: Int) {
        if ((seen[orderId] ?: 0) >= lastId) return
        seen = seen + (orderId to lastId)
        clientId?.let { AppStorage.saveChatSeen(it, seen) }
    }

    // Abre el panel en el chat con el restaurante (por ejemplo desde una notificación)
    fun openChat(orderId: Int?) {
        openOrderId = orderId
        ChatStore.mode = ChatMode.SUPPORT
        ChatStore.open = true
    }
}

// Estados del pedido que siguen en curso (el chat se cierra al entregar o cancelar)
private val CLOSED_STATUSES = setOf("DELIVERED", "CANCELLED")

private fun statusLabel(status: String): String = when (status) {
    "PENDING" -> t("Pendiente")
    "CONFIRMED" -> t("Confirmado")
    "PREPARING" -> t("En cocina")
    "ON_THE_WAY" -> t("En camino")
    else -> status
}

// Contenido del panel en modo "Soporte": elegir pedido y chatear con el restaurante
@Composable
fun ColumnScope.SupportChatContent() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Pedidos en curso del cliente (para poder escribir aunque aún no haya mensajes)
    var activeOrders by remember { mutableStateOf<List<Pair<Int, String>>?>(null) }
    var ordersError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        SupportStore.refreshNow()
        try {
            activeOrders = ApiClient.api.myOrders()
                .filter { it.status !in CLOSED_STATUSES }
                .sortedByDescending { it.id }
                .map { it.id to it.status }
            ordersError = false
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            ordersError = true
            activeOrders = emptyList()
        }
        // Si no hay uno elegido: el que tiene mensajes nuevos, el último con chat o el más reciente
        if (SupportStore.openOrderId == null) {
            SupportStore.openOrderId = SupportStore.unreadIds.firstOrNull()
                ?: SupportStore.summaries.firstOrNull()?.orderId
                ?: activeOrders?.firstOrNull()?.first
        }
    }

    val options = buildList {
        activeOrders?.forEach { add(it.first) }
        SupportStore.summaries.forEach { if (it.orderId !in this) add(it.orderId) }
        SupportStore.openOrderId?.let { if (it !in this) add(0, it) }
    }

    // Selector de pedido (solo si hay más de uno)
    if (options.size > 1) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            options.forEach { id ->
                val selected = id == SupportStore.openOrderId
                Box {
                    Text(
                        t("Pedido #{}", id),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selected) Color.White else Terra,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) Terra else Color.Transparent)
                            .border(1.dp, if (selected) Terra else SupportLine, RoundedCornerShape(50))
                            .clickable { SupportStore.openOrderId = id }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                    if (id in SupportStore.unreadIds) UnreadDot(Modifier.align(Alignment.TopEnd))
                }
            }
        }
    }

    val orderId = SupportStore.openOrderId
    when {
        activeOrders == null -> CenterNote(t("Cargando tus pedidos…"))
        orderId == null -> CenterNote(
            if (ordersError) t("No pudimos cargar tus pedidos. Revisa tu conexión.")
            else t("No tienes pedidos en curso. Cuando hagas un pedido podrás escribirle al restaurante aquí.")
        )
        else -> OrderConversation(
            orderId = orderId,
            status = activeOrders?.firstOrNull { it.first == orderId }?.second
                ?: SupportStore.summaries.firstOrNull { it.orderId == orderId }?.orderStatus,
            scope = scope,
            context = context
        )
    }
}

@Composable
private fun ColumnScope.CenterNote(text: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        Text(text, fontSize = 14.sp, color = SupportMuted, lineHeight = 20.sp)
    }
}

@Composable
private fun ColumnScope.OrderConversation(
    orderId: Int,
    status: String?,
    scope: CoroutineScope,
    context: android.content.Context
) {
    var messages by remember(orderId) { mutableStateOf<List<OrderChatMessage>>(emptyList()) }
    var chatOpen by remember(orderId) { mutableStateOf(true) }
    var loadFailures by remember(orderId) { mutableStateOf(0) }
    var draft by remember(orderId) { mutableStateOf("") }
    var sending by remember(orderId) { mutableStateOf(false) }
    var error by remember(orderId) { mutableStateOf<String?>(null) }
    // Foto elegida y aún sin enviar (ya reducida a JPG)
    var pendingPhoto by remember(orderId) { mutableStateOf<ByteArray?>(null) }
    var preparing by remember(orderId) { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Lee los mensajes cada 3 s (solo los nuevos), como la web
    LaunchedEffect(orderId) {
        var lastId: Int? = null
        while (isActive) {
            try {
                val chat = ApiClient.api.orderChatMessages(orderId, lastId)
                loadFailures = 0
                chatOpen = chat.open
                if (chat.messages.isNotEmpty()) {
                    val known = messages.map { it.id }.toSet()
                    messages = messages + chat.messages.filter { it.id !in known }
                    lastId = messages.last().id
                    SupportStore.markSeen(orderId, lastId)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val code = (e as? retrofit2.HttpException)?.code()
                val body = e.apiError()
                // Pedido sin pagar, ajeno o inexistente: el back explica por qué no hay chat
                if (body?.message != null && code != null && code in setOf(403, 404, 409)) {
                    error = body.message
                    chatOpen = false
                }
                loadFailures++
            }
            delay(3_000)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    // Reduce la foto elegida o tomada y la deja lista para enviar
    fun usePhoto(uri: Uri, deleteAfter: java.io.File? = null) {
        preparing = true
        error = null
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                preparePhoto(context, uri).also { deleteAfter?.delete() }
            }
            preparing = false
            if (bytes == null) error = t("No pudimos procesar la foto. Prueba con otra.")
            else pendingPhoto = bytes
        }
    }

    // Galería: el selector de fotos de Android no necesita permiso de almacenamiento
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) usePhoto(uri)
    }

    // Cámara: guarda la foto en la carpeta temporal y luego se usa como cualquier otra
    var cameraFile by remember { mutableStateOf<java.io.File?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val file = cameraFile
        if (taken && file != null) {
            usePhoto(androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file), file)
        } else {
            file?.delete()
        }
    }

    fun openCamera() {
        try {
            val dir = java.io.File(context.cacheDir, "chat_photos").apply { mkdirs() }
            val file = java.io.File(dir, "foto_${System.currentTimeMillis()}.jpg")
            cameraFile = file
            camera.launch(
                androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            )
        } catch (_: Exception) {
            error = t("No pudimos abrir la cámara.")
        }
    }

    // El permiso de cámara se pide una sola vez: si ya se dio, abre directo; si se
    // negó, Android no vuelve a preguntar y se explica cómo activarlo
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) openCamera()
        else error = t("Sin permiso de cámara no podemos tomar la foto. Puedes activarlo en los ajustes del teléfono.")
    }

    fun takePhoto() {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) openCamera() else cameraPermission.launch(android.Manifest.permission.CAMERA)
    }

    var attachMenu by remember(orderId) { mutableStateOf(false) }

    fun send() {
        val text = draft.trim()
        val photo = pendingPhoto
        if (sending || preparing || !chatOpen || (photo == null && text.isEmpty())) return
        sending = true
        error = null
        scope.launch {
            try {
                val sent = if (photo != null) {
                    val part = MultipartBody.Part.createFormData(
                        "file", "foto.jpg", photo.toRequestBody("image/jpeg".toMediaType())
                    )
                    ApiClient.chatApi.sendOrderChatPhoto(
                        orderId, part,
                        text.takeIf { it.isNotEmpty() }?.toRequestBody("text/plain".toMediaType())
                    )
                } else {
                    ApiClient.api.sendOrderChat(orderId, SendChatMessage(text))
                }
                draft = ""
                pendingPhoto = null
                if (messages.none { it.id == sent.id }) messages = messages + sent
                SupportStore.markSeen(orderId, sent.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val http = e as? retrofit2.HttpException
                val message = e.apiError()?.message
                if (http?.code() == 409 && photo == null) {
                    chatOpen = false
                    error = message
                } else {
                    error = message ?: if (photo != null) t("No se pudo enviar la foto. Inténtalo de nuevo.")
                    else t("No se pudo enviar el mensaje. Inténtalo de nuevo.")
                }
            } finally {
                sending = false
            }
        }
    }

    // Estado del pedido
    Text(
        t("Pedido #{}", orderId) + (status?.let { " - ${statusLabel(it)}" } ?: ""),
        fontSize = 12.sp,
        color = SupportMuted,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp)
    )

    LazyColumn(
        state = listState,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (messages.isEmpty()) {
            item {
                Text(
                    t("Cuéntanos qué pasa con tu pedido y un operador te responderá."),
                    fontSize = 14.sp,
                    color = SupportMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SupportSoft)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
        items(messages, key = { it.id }) { m -> SupportBubble(orderId, m) }
    }

    if (loadFailures >= 2 && chatOpen) {
        Text(
            t("No pudimos actualizar el chat. Lo seguimos intentando…"),
            fontSize = 12.sp, color = SupportMuted,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
    error?.let {
        com.example.terragalegamovile.ui.components.ErrorNotice(it, Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }

    if (!chatOpen) {
        Text(
            t("El pedido ya finalizó, el chat está cerrado."),
            fontSize = 13.sp,
            color = SupportMuted,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SupportLine)
                .padding(16.dp)
        )
        return
    }

    // Foto por enviar
    pendingPhoto?.let { bytes ->
        val bitmap = remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            bitmap?.let {
                Image(
                    it, contentDescription = t("Vista previa de la foto que vas a enviar"),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(t("Foto lista para enviar. Puedes agregar un comentario."), fontSize = 12.sp, color = SupportMuted, modifier = Modifier.weight(1f))
            Text(
                t("Quitar"), color = TerraText, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { pendingPhoto = null }.padding(8.dp)
            )
        }
    }

    // Adjuntar foto, escribir y enviar
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SupportLine)
            // Más aire abajo: la barra queda un poco más arriba del borde
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 22.dp)
    ) {
        Box {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !sending && !preparing) { attachMenu = true }
                    .semantics { contentDescription = t("Adjuntar foto") }
            ) {
                if (preparing) Text("…", color = TerraText, fontSize = 20.sp) else CameraIcon(Terra, 27.dp)
            }
            // Tomar una foto ahora o elegir una de la galería
            androidx.compose.material3.DropdownMenu(
                expanded = attachMenu,
                onDismissRequest = { attachMenu = false },
                containerColor = Paper,
                // Tarjeta redondeada
                shape = RoundedCornerShape(18.dp)
            ) {
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(t("Tomar foto"), color = Charcoal) },
                    leadingIcon = { CameraIcon(Terra) },
                    onClick = { attachMenu = false; takePhoto() }
                )
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(t("Elegir de la galería"), color = Charcoal) },
                    leadingIcon = { GalleryIcon(Terra) },
                    onClick = {
                        attachMenu = false
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it.take(SUPPORT_MAX) },
            placeholder = {
                Text(
                    if (pendingPhoto != null) t("Agrega un comentario (opcional)…") else t("Escribe un mensaje…"),
                    color = SupportMuted
                )
            },
            shape = RoundedCornerShape(50),
            maxLines = 4,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Send
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { send() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SupportSoft,
                unfocusedContainerColor = SupportSoft,
                focusedBorderColor = Terra,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Charcoal,
                unfocusedTextColor = Charcoal,
                cursorColor = Terra
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 54.dp)
        )
        Spacer(Modifier.width(8.dp))
        val canSend = !sending && !preparing && (pendingPhoto != null || draft.isNotBlank())
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (canSend) Terra else Terra.copy(alpha = 0.4f))
                .clickable(enabled = canSend) { send() }
                .semantics { contentDescription = t("Enviar") }
        ) {
            Text(if (sending) "…" else "➜", color = Color.White, fontSize = 20.sp)
        }
    }
}

@Composable
private fun SupportBubble(orderId: Int, m: OrderChatMessage) {
    val mine = m.senderRole == "CLIENT"
    Column(
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!mine) {
            Text(
                t("Restaurante · {}", m.senderName),
                fontSize = 12.sp, color = SupportMuted,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (mine) 16.dp else 5.dp,
                        bottomEnd = if (mine) 5.dp else 16.dp
                    )
                )
                .background(if (mine) Terra else SupportSoft)
                .padding(if (m.attachmentId != null) 4.dp else 0.dp)
        ) {
            m.attachmentId?.let { attachment ->
                ChatPhoto(orderId, attachment, mine)
            }
            m.content?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    fontSize = 14.sp, lineHeight = 20.sp,
                    color = if (mine) Color.White else Charcoal,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
        Text(
            timeLabel(m.createdAt),
            fontSize = 11.sp, color = SupportMuted,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

// La foto se pide con el token (el endpoint no es público)
@Composable
private fun ChatPhoto(orderId: Int, attachmentId: Int, mine: Boolean) {
    val context = LocalContext.current
    val token = Session.validToken()
    val request = remember(orderId, attachmentId, token) {
        ImageRequest.Builder(context)
            .data("${ApiClient.BASE_URL}order-chat/$orderId/attachments/$attachmentId")
            .apply { if (token != null) addHeader("Authorization", "Bearer $token") }
            .crossfade(true)
            .build()
    }
    SubcomposeAsyncImage(
        model = request,
        contentDescription = if (mine) t("Foto que enviaste") else t("Foto enviada por el restaurante"),
        contentScale = ContentScale.Crop,
        loading = {
            Box(Modifier.size(200.dp, 150.dp), contentAlignment = Alignment.Center) {
                Text(t("Cargando foto…"), fontSize = 12.sp, color = if (mine) Color.White else SupportMuted)
            }
        },
        error = {
            Box(Modifier.size(200.dp, 80.dp), contentAlignment = Alignment.Center) {
                Text(t("No se pudo cargar la foto."), fontSize = 12.sp, color = if (mine) Color.White else SupportMuted)
            }
        },
        modifier = Modifier
            .widthIn(max = 260.dp)
            .heightIn(max = 260.dp)
            .clip(RoundedCornerShape(12.dp))
    )
}

// "2026-11-22T10:15:30" -> "10:15" (sin java.time, que pide Android 8)
private fun timeLabel(createdAt: String): String =
    createdAt.substringAfter('T', "").take(5)

// Reduce la foto (máx. 1600 px) y la pasa a JPG para no superar los 5 MB del back
private fun preparePhoto(context: android.content.Context, uri: Uri): ByteArray? = try {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / sample > 3200 || bounds.outHeight / sample > 3200) sample *= 2
    val decoded = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: throw IllegalStateException()
    val scale = minOf(1f, 1600f / maxOf(decoded.width, decoded.height))
    val bitmap = if (scale < 1f) {
        Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
    } else decoded
    var quality = 85
    var bytes: ByteArray
    do {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        bytes = out.toByteArray()
        quality -= 15
    } while (bytes.size > 4_500_000 && quality > 30)
    bytes
} catch (_: Exception) {
    null
}

// Puntito de mensajes nuevos
@Composable
fun UnreadDot(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(Color(0xFF8A2222))
            .border(1.5.dp, Color.White, CircleShape)
    )
}

// Auriculares con micrófono (el mismo ícono de "Operador" de la web)
@Composable
fun HeadsetIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val s = w / 24f
        val stroke = Stroke(width = 2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val arc = Path().apply {
            moveTo(3f * s, 14f * s); lineTo(3f * s, 12f * s)
            cubicTo(3f * s, 7f * s, 7f * s, 3f * s, 12f * s, 3f * s)
            cubicTo(17f * s, 3f * s, 21f * s, 7f * s, 21f * s, 12f * s)
            lineTo(21f * s, 14f * s)
        }
        drawPath(arc, color, style = stroke)
        val right = Path().apply {
            moveTo(21f * s, 15f * s)
            cubicTo(21f * s, 16.1f * s, 20.1f * s, 17f * s, 19f * s, 17f * s)
            lineTo(18f * s, 17f * s); lineTo(18f * s, 12f * s); lineTo(19f * s, 12f * s)
            cubicTo(20.1f * s, 12f * s, 21f * s, 12.9f * s, 21f * s, 14f * s); close()
        }
        drawPath(right, color, style = stroke)
        val left = Path().apply {
            moveTo(3f * s, 15f * s)
            cubicTo(3f * s, 16.1f * s, 3.9f * s, 17f * s, 5f * s, 17f * s)
            lineTo(6f * s, 17f * s); lineTo(6f * s, 12f * s); lineTo(5f * s, 12f * s)
            cubicTo(3.9f * s, 12f * s, 3f * s, 12.9f * s, 3f * s, 14f * s); close()
        }
        drawPath(left, color, style = stroke)
        val mic = Path().apply {
            moveTo(18f * s, 17f * s); lineTo(18f * s, 18f * s)
            cubicTo(18f * s, 19.7f * s, 16.7f * s, 21f * s, 15f * s, 21f * s)
            lineTo(12f * s, 21f * s)
        }
        drawPath(mic, color, style = stroke)
    }
}

// Foto (marco con montaña y sol) para "Elegir de la galería"
@Composable
private fun GalleryIcon(color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color,
            topLeft = Offset(w * 0.1f, w * 0.16f),
            size = androidx.compose.ui.geometry.Size(w * 0.8f, w * 0.68f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.1f),
            style = stroke
        )
        val hill = Path().apply {
            moveTo(w * 0.14f, w * 0.76f)
            lineTo(w * 0.4f, w * 0.5f)
            lineTo(w * 0.58f, w * 0.66f)
            lineTo(w * 0.7f, w * 0.56f)
            lineTo(w * 0.88f, w * 0.74f)
        }
        drawPath(hill, color, style = stroke)
        drawCircle(color, radius = w * 0.07f, center = Offset(w * 0.68f, w * 0.34f))
    }
}

@Composable
private fun CameraIcon(color: Color, iconSize: androidx.compose.ui.unit.Dp = 22.dp) {
    Canvas(Modifier.size(iconSize)) {
        val w = size.width
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val body = Path().apply {
            moveTo(w * 0.08f, w * 0.32f)
            lineTo(w * 0.30f, w * 0.32f); lineTo(w * 0.38f, w * 0.18f); lineTo(w * 0.62f, w * 0.18f)
            lineTo(w * 0.70f, w * 0.32f); lineTo(w * 0.92f, w * 0.32f); lineTo(w * 0.92f, w * 0.84f)
            lineTo(w * 0.08f, w * 0.84f); close()
        }
        drawPath(body, color, style = stroke)
        drawCircle(color, radius = w * 0.16f, center = Offset(w * 0.5f, w * 0.57f), style = stroke)
    }
}
