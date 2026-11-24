    package com.example.terragalegamovile

import com.example.terragalegamovile.data.t

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.example.terragalegamovile.ui.ChatSheet
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.blur
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.terragalegamovile.data.ApiClient
import com.example.terragalegamovile.data.AppStorage
import com.example.terragalegamovile.data.CartStore
import com.example.terragalegamovile.data.AlertsService
import com.example.terragalegamovile.data.CheckoutDraft
import com.example.terragalegamovile.data.ClientUi
import com.example.terragalegamovile.data.DeliveryNotifier
import com.example.terragalegamovile.data.MapService
import com.example.terragalegamovile.data.NotificationStore
import com.example.terragalegamovile.data.Session
import com.example.terragalegamovile.ui.CartSheet
import com.example.terragalegamovile.ui.CategoryScreen
import com.example.terragalegamovile.ui.CheckoutConfirmScreen
import com.example.terragalegamovile.ui.CompleteProfileScreen
import com.example.terragalegamovile.ui.WaiterScreen
import com.example.terragalegamovile.ui.WaiterUi
import com.example.terragalegamovile.ui.CheckoutScreen
import com.example.terragalegamovile.ui.ClientHomeScreen
import com.example.terragalegamovile.ui.ClientScreen
import com.example.terragalegamovile.ui.CourierScreen
import com.example.terragalegamovile.ui.LoginScreen
import com.example.terragalegamovile.ui.ProductScreen
import com.example.terragalegamovile.ui.ProfileScreen
import com.example.terragalegamovile.ui.components.ProfileNav
import com.example.terragalegamovile.ui.theme.TerraGalegaMovileTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.example.terragalegamovile.ui.NotificationsScreen
import com.example.terragalegamovile.ui.components.NotificationsNav
import com.example.terragalegamovile.ui.CouponsScreen
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa el almacenamiento y recupera una sesión vigente.
        AppStorage.init(applicationContext)
        // Idioma de la app (español, inglés o catalán)
        com.example.terragalegamovile.data.I18n.init(applicationContext)
        com.example.terragalegamovile.data.NetworkState.start(applicationContext)
        // Modo oscuro: el que se eligió en la app o, si no, el del teléfono
        com.example.terragalegamovile.ui.theme.ThemeState.systemDark =
            (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        com.example.terragalegamovile.ui.theme.ThemeState.mode = AppStorage.loadTheme()
        MapService.init(applicationContext)
        Session.restore()

        CartStore.bind(
            Session.user?.takeIf { it.role == "CLIENT" }?.id
        )

        // Avisos de "tu pedido salió" y "ya va llegando" (notificación + vibración)
        DeliveryNotifier.start(applicationContext)
        NotificationStore.start(applicationContext)
        // Chat con el restaurante: qué pedidos tienen mensajes nuevos del operador
        com.example.terragalegamovile.ui.SupportStore.start()
        handleNotificationIntent(intent)

        enableEdgeToEdge()

        setContent {
            TerraGalegaMovileTheme {

                // Sincroniza el carrito mientras la actividad está visible.
                LaunchedEffect(lifecycle) {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        while (isActive) {
                            delay(6_000)
                            CartStore.refresh()
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        AppNav(lifecycle = lifecycle)
                    }
                }
            }
        }
    }

    // La app ya estaba abierta (singleTask) y el usuario tocó una notificación
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    // Tocar la notificación de domicilio abre la pantalla de seguimiento
    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        // Enlace "recuperar contraseña" del correo abierto con la app
        val data = intent.data
        if (intent.action == Intent.ACTION_VIEW && data != null &&
            data.path?.startsWith("/reset-password") == true
        ) {
            data.getQueryParameter("token")?.takeIf { it.isNotBlank() }?.let {
                com.example.terragalegamovile.ui.PasswordResetLink.token.value = it
            }
            intent.data = null
        }
        // Aviso de cocina del mesero: abre la mesa y marca el pedido
        if (intent.hasExtra(NotificationStore.EXTRA_TABLE_ID)) {
            val orderId = intent.getIntExtra(NotificationStore.EXTRA_ORDER_ID, -1).takeIf { it >= 0 }
            WaiterUi.focusOrder = orderId
            WaiterUi.openTable = intent.getIntExtra(NotificationStore.EXTRA_TABLE_ID, -1).takeIf { it >= 0 }
            intent.removeExtra(NotificationStore.EXTRA_TABLE_ID)
            intent.removeExtra(NotificationStore.EXTRA_ORDER_ID)
        }
        if (intent.getBooleanExtra(DeliveryNotifier.EXTRA_OPEN_TRACKING, false)) {
            DeliveryNotifier.openTrackingRequest.value = true
            intent.removeExtra(DeliveryNotifier.EXTRA_OPEN_TRACKING)
        }
    }
}

@Composable
fun AppNav(lifecycle: Lifecycle) {
    val nav = rememberNavController()

    // Las pantallas se actualizan cuando cambia la sesión.
    val session by Session.state.collectAsState()

    var showCart by remember { mutableStateOf(false) }

    // Se calcula una sola vez para inicializar la navegación.
    val startDestination = remember {
        when (session?.role) {
            "CLIENT" -> "client"
            "DELIVERY_PERSON" -> "courier"
            "WAITER" -> "waiter"
            else -> "login"
        }
    }

    // El efecto de cierre de sesión se encarga de limpiar y navegar.
    val logout: () -> Unit = {
        com.example.terragalegamovile.ui.ChatStore.forget()
        Session.user = null
    }

    // Vuelve a la pestaña Carta del inicio del cliente.
    val goMenu: () -> Unit = {
        showCart = false
        ClientUi.tab = 0
        nav.popBackStack("client", false)
    }

    // La inicial de la barra superior abre el perfil.
    // El callback consulta la sesión vigente al pulsar el botón.
    DisposableEffect(Unit) {
        ProfileNav.open = {
            if (
                Session.user?.role == "CLIENT" &&
                nav.currentDestination?.route != "profile"
            ) {
                nav.navigate("profile")
            }
        }

        onDispose {
            ProfileNav.open = null
        }
    }

    // El ícono de ayuda de la barra abre el asistente virtual (solo clientes): un panel
    // encima de la pantalla actual, no otra pantalla.
    DisposableEffect(Unit) {
        com.example.terragalegamovile.ui.components.ChatNav.open = {
            if (Session.user?.role == "CLIENT") {
                val support = com.example.terragalegamovile.ui.SupportStore
                if (support.hasUnread) support.openChat(support.unreadIds.first())
                else com.example.terragalegamovile.ui.ChatStore.open = true
            }
        }

        onDispose {
            com.example.terragalegamovile.ui.components.ChatNav.open = null
        }
    }

    // La campanita de la barra abre la bandeja de notificaciones.
    DisposableEffect(Unit) {
        NotificationsNav.open = {
            if (
                (Session.user?.role == "CLIENT" || Session.user?.role == "WAITER") &&
                nav.currentDestination?.route != "notifications"
            ) {
                nav.navigate("notifications")
            }
        }

        onDispose {
            NotificationsNav.open = null
        }
    }

    // Tocar un aviso de cocina con la bandeja abierta: vuelve al portal para abrir la mesa
    LaunchedEffect(WaiterUi.openTable) {
        if (WaiterUi.openTable != null &&
            session?.role == "WAITER" &&
            nav.currentDestination?.route != "waiter"
        ) {
            nav.popBackStack("waiter", false)
        }
    }

    // Cierre manual o automático:
    // limpia los datos de pantalla y elimina la navegación privada.
    LaunchedEffect(session == null) {
        if (session == null) {
            showCart = false
            CartStore.bind(null)
            CheckoutDraft.reset()
            ClientUi.tab = 0
            ClientUi.paid = false

            if (nav.currentDestination?.route != "login") {
                nav.navigate("login") {
                    popUpTo(nav.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }
    }

    // Comprueba la sesión mientras la actividad está visible.
    // Al volver a la app se comprueba de nuevo inmediatamente.
    LaunchedEffect(session?.token, lifecycle) {
        if (session == null) return@LaunchedEffect

        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                // Detecta el vencimiento incluso sin conexión.
                if (Session.validToken() == null) {
                    break
                }

                // Al token le queda poco: se pide uno nuevo sin cerrar la sesión
                Session.renewIfNeeded()

                try {
                    ApiClient.api.checkSession().close()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // ApiClient procesa los 401 y elimina la sesión.
                    // Un fallo de conexión o un error del servidor
                    // no deben cerrar la sesión por sí solos.
                }

                delay(15_000)
            }
        }
    }

    // Android 13+: los avisos de domicilio necesitan el permiso de notificaciones.
    // Se pide una vez, cuando entra un cliente.
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    // Antes del permiso se explica para qué sirve (una sola vez); luego Android pregunta
    var askNotifications by remember { mutableStateOf(false) }
    LaunchedEffect(session?.role) {
        if ((session?.role == "CLIENT" || session?.role == "WAITER") &&
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (AppStorage.notificationsExplained()) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                askNotifications = true
            }
        }
        // Cliente y mesero: "avisos activos" para que todas las notificaciones lleguen
        // con la app cerrada. Se inicia aquí, con la app abierta (Android no deja
        // hacerlo desde segundo plano); después sigue solo, incluso tras reiniciar.
        if (AlertsService.shouldRun(session?.role)) {
            AlertsService.start(context)
        } else {
            AlertsService.stop(context)
        }
    }

    if (askNotifications) {
        val waiter = session?.role == "WAITER"
        com.example.terragalegamovile.ui.components.TerraDialog(
            title = t("Activa las notificaciones"),
            message = if (waiter) t("Te avisaremos al instante cuando la cocina empiece o termine los platos de tus mesas.")
            else t("Te avisaremos cuando confirmen tu pedido, cuando salga hacia tu casa y cuando esté por llegar."),
            confirmLabel = t("Activar"),
            onConfirm = {
                askNotifications = false
                AppStorage.setNotificationsExplained()
                if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            cancelLabel = t("Ahora no"),
            onCancel = {
                askNotifications = false
                AppStorage.setNotificationsExplained()
            }
        )
    }

    // Se tocó una notificación de domicilio: abre el seguimiento del cliente
    val openTracking by DeliveryNotifier.openTrackingRequest.collectAsState()

    LaunchedEffect(openTracking, session?.role) {
        if (!openTracking) return@LaunchedEffect

        if (session?.role == "CLIENT") {
            if (nav.currentDestination?.route != "tracking") {
                nav.navigate("tracking") { launchSingleTop = true }
            }
            DeliveryNotifier.openTrackingRequest.value = false
        } else if (session == null) {
            // Sin sesión no hay pedido que mostrar: se descarta la solicitud
            DeliveryNotifier.openTrackingRequest.value = false
        }
    }

    val chatOpen = com.example.terragalegamovile.ui.ChatStore.open && session?.role == "CLIENT"
    val chatBlur by androidx.compose.animation.core.animateDpAsState(
        if (chatOpen) 14.dp else 0.dp, label = "chatBlur"
    )
    androidx.compose.foundation.layout.Box(
        androidx.compose.ui.Modifier
            .fillMaxSize()
            .then(if (chatBlur > 0.dp) androidx.compose.ui.Modifier.blur(chatBlur) else androidx.compose.ui.Modifier)
    ) {
    NavHost(
        navController = nav,
        startDestination = startDestination,
        // Al entrar: aparece y se desliza un poco desde la derecha; al volver, al revés
        enterTransition = {
            androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)) +
                androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(220)) { it / 10 }
        },
        exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)) },
        popEnterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)) },
        popExitTransition = {
            androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(180)) +
                androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(220)) { it / 10 }
        }
    ) {

        composable("login") {
            LoginScreen { user ->
                // LoginScreen ya guarda el usuario en Session.user.
                CartStore.bind(
                    user.takeIf { it.role == "CLIENT" }?.id
                )

                val destination = when (user.role) {
                    "CLIENT" -> "client"
                    "DELIVERY_PERSON" -> "courier"
                    "WAITER" -> "waiter"
                    else -> null
                }

                if (destination != null) {
                    nav.navigate(destination) {
                        popUpTo("login") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                } else {
                    Session.user = null
                }
            }
        }

        composable("client") {
            session?.let { user ->
                // Cuenta creada con Google sin terminar: primero completa su perfil.
                // Al guardar cambia Session.user y aquí se muestra el inicio.
                if (user.profileComplete == false) {
                    CompleteProfileScreen(user = user, onLogout = logout)
                    return@let
                }
                ClientHomeScreen(
                    user = user,
                    onLogout = logout,
                    onOpenProduct = { id ->
                        nav.navigate("product/$id")
                    },
                    onOpenCategory = { name ->
                        nav.navigate("category/${Uri.encode(name)}")
                    },
                    onOpenCart = {
                        showCart = true
                    },
                    onTrack = {
                        nav.navigate("tracking")
                    }
                )
            }
        }

        composable(
            route = "category/{name}",
            arguments = listOf(
                navArgument("name") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val name = entry.arguments?.getString("name")
            val user = session

            if (name != null && user != null) {
                CategoryScreen(
                    user = user,
                    category = name,
                    onBack = {
                        nav.popBackStack()
                    },
                    onOpenProduct = { id ->
                        nav.navigate("product/$id")
                    },
                    onOpenCart = {
                        showCart = true
                    },
                    onLogout = logout
                )
            }
        }

        composable("notifications") {
            session?.let { user ->
                NotificationsScreen(
                    user = user,
                    onBack = { nav.popBackStack() },
                    onLogout = logout,
                    onOpen = { n ->
                        NotificationStore.markRead(n.id)
                        when (n.type) {
                            // Aviso de cocina: abre la mesa en el portal del mesero
                            "DISH_PREPARING", "DISH_READY", "ORDER_READY" -> {
                                WaiterUi.focusOrder = n.orderId
                                WaiterUi.openTable = n.tableId
                                nav.popBackStack("waiter", false)
                            }
                            "ORDER_ON_THE_WAY", "ORDER_NEAR" -> nav.navigate("tracking")
                            "COUPON" -> nav.navigate("coupons")
                            // El operador escribió: abre el chat de ese pedido
                            "ORDER_CHAT_MESSAGE" -> com.example.terragalegamovile.ui.SupportStore.openChat(n.orderId)
                            "ORDER_CONFIRMED", "ORDER_PREPARING" -> {
                                ClientUi.tab = 1
                                nav.popBackStack("client", false)
                            }
                            else -> goMenu()
                        }
                    }
                )
            }
        }

        composable(
            route = "product/{id}",
            arguments = listOf(
                navArgument("id") {
                    type = NavType.IntType
                }
            )
        ) { entry ->
            val id = entry.arguments?.getInt("id")
            val user = session

            if (id != null && user != null) {
                ProductScreen(
                    user = user,
                    productId = id,
                    onBack = {
                        nav.popBackStack()
                    },
                    onOpenProduct = { other ->
                        nav.navigate("product/$other")
                    },
                    onOpenCart = {
                        showCart = true
                    },
                    onLogout = logout
                )
            }
        }

        composable("coupons") {
            session?.let { user ->
                CouponsScreen(
                    user = user,
                    onBack = { nav.popBackStack() },
                    onLogout = logout
                )
            }
        }

        composable("tracking") {
            session?.let { user ->
                ClientScreen(
                    user,
                    logout,
                    onBack = {
                        nav.popBackStack()
                    }
                )
            }
        }

        composable("checkout") {
            session?.let { user ->
                CheckoutScreen(
                    user = user,
                    onLogout = logout,
                    onContinue = {
                        nav.navigate("checkout/confirm")
                    },
                    onExit = goMenu
                )
            }
        }

        composable("checkout/confirm") {
            session?.let { user ->
                CheckoutConfirmScreen(
                    user = user,
                    onLogout = logout,
                    onEdit = {
                        nav.popBackStack()
                    },
                    onDone = {
                        nav.popBackStack("client", false)
                    },
                    onExit = goMenu
                )
            }
        }

        composable("profile") {
            session?.let { user ->
                ProfileScreen(
                    user = user,
                    onBack = {
                        nav.popBackStack()
                    },
                    onLogout = logout,
                    onOpenCoupons = { nav.navigate("coupons") }

                )
            }
        }

        composable("courier") {
            session?.let { user ->
                CourierScreen(
                    user,
                    logout
                )
            }
        }

        // Portal del mesero: pisos, mesas y pedidos de mesa
        composable("waiter") {
            session?.let { user ->
                WaiterScreen(user = user, onLogout = logout)
            }
        }
    }

    }

    // Asistente virtual: panel encima de todo con el fondo desenfocado
    androidx.compose.animation.AnimatedVisibility(
        visible = chatOpen,
        enter = androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.fadeOut()
    ) {
        com.example.terragalegamovile.ui.ChatScrim { com.example.terragalegamovile.ui.ChatStore.open = false }
    }
    androidx.compose.animation.AnimatedVisibility(
        visible = chatOpen,
        enter = androidx.compose.animation.slideInVertically { it },
        exit = androidx.compose.animation.slideOutVertically { it }
    ) {
        androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize()) {
            session?.let { user ->
                ChatSheet(
                    clientId = user.id,
                    firstName = user.name,
                    onClose = { com.example.terragalegamovile.ui.ChatStore.open = false }
                )
            }
        }
    }

    // El carrito solo puede mostrarse con una sesión de cliente.
    if (showCart && session?.role == "CLIENT") {
        CartSheet(
            onClose = {
                showCart = false
            },
            onCheckout = {
                showCart = false
                nav.navigate("checkout")
            },
            onGoMenu = goMenu
        )
    }
}