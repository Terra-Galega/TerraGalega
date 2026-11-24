# Terra Galega 🍽️

**Terra Galega** es un restaurante de **comida gallega** ubicado en Bogotá. En los últimos años ha ganado gran popularidad y, aunque cuenta con una única sede, la cantidad de pedidos ha crecido año tras año.

Este repositorio contiene la plataforma digital del restaurante, desarrollada como proyecto del curso **Desarrollo Web** de la **Pontificia Universidad Javeriana**. Está compuesta por una aplicación web, una aplicación móvil Android, un backend con API REST y un asistente de atención por WhatsApp.

🌐 **Aplicación en producción:** https://terragalega.up.railway.app

## El negocio

Antes, el restaurante manejaba los domicilios de forma completamente manual: el cliente llamaba por teléfono, un trabajador anotaba el pedido a mano y el envío se completaba de forma algo desorganizada. Terra Galega digitalizó este proceso con una plataforma que le permite:

- **Promocionar sus comidas**, mostrando la carta y la propuesta gastronómica del restaurante.
- **Permitir a los clientes hacer pedidos y reservas** de forma autónoma, desde la web, la app o WhatsApp, y pagarlos en línea.
- **Permitir a los trabajadores monitorear cada pedido**, desde que se paga hasta que el domiciliario lo entrega.
- **Atender el salón**: reservas de mesa, pedidos de mesa tomados por los meseros y tablero de cocina.

La propuesta gastronómica de Terra Galega está centrada en la cocina tradicional de Galicia (España): entradas, mariscos, carnes, postres y bebidas típicas de esa región.

## Tipos de usuario

| Rol | Plataforma | Descripción |
|---|---|---|
| **Visitante** | Web | Ve la página de inicio, la carta y el detalle de los productos. Para pagar un pedido o una reserva debe registrarse o iniciar sesión. |
| **Cliente** | Web, app móvil y WhatsApp | Se registra con correo o con Google. Arma su carrito, aplica cupones y paga en efectivo, con tarjeta (Stripe) o con puntos. Sigue en el mapa la ubicación del domiciliario y le entrega un PIN. Reserva mesa pagando un anticipo, se suscribe a Premium, escribe reseñas, conversa con el asistente **Galleguito** y chatea con el operador si hay un problema con su pedido. |
| **Operador** | Web | Confirma los pedidos, actualiza su estado y asigna un domiciliario disponible. Sigue los domicilios en curso, atiende el chat con los clientes, consulta las reservas en el plano de mesas y recibe las solicitudes de contacto que llegan por WhatsApp. |
| **Cocina** | Web | Ve los pedidos de mesa que envían los meseros y actualiza el estado de cada plato. |
| **Mesero** | App móvil | Consulta las mesas por piso y las reservas del día, toma los pedidos de cada mesa, los envía a cocina, los entrega y registra el cobro. |
| **Domiciliario** | App móvil | Ve los domicilios que tiene asignados, inicia el trayecto con la ruta en el mapa, comparte su ubicación y cierra la entrega ingresando el PIN del cliente. |
| **Administrador** | Web | Gestiona la carta (categorías, adicionales y productos), los anuncios, los cupones, las cuentas del personal y las reseñas. Consulta el panel de indicadores con reportes exportables a PDF y Excel. |

## El menú

- Cada **producto** tiene nombre, precio, descripción, imagen, categoría y características: popular, vegetariano, nivel de picante y alérgenos (frutos secos, mariscos, gluten).
- Los **adicionales** se asocian a las **categorías**, no a los productos. Un mismo adicional puede ofrecerse en varias categorías, y todos los productos de esa categoría lo heredan.
- Cuando un cliente quiere varias unidades de un mismo producto, se registra como una cantidad dentro del pedido.
- Un producto que ya tiene pedidos no se elimina: se **desactiva**, y deja de mostrarse en la carta sin afectar el historial.

## Formas de pago

- **Efectivo** contra entrega.
- **Tarjeta**, a través de **Stripe**.
- **Puntos**, solo para clientes Premium con saldo suficiente.

La suscripción **Premium** cuesta $65.000 y dura 30 días. Da domicilios gratis y permite acumular y usar puntos. No se renueva automáticamente.

## El ciclo de vida de un pedido

1. El cliente agrega productos al carrito, con sus adicionales y cantidades. El carrito se guarda en el servidor: si cierra la página o cambia de dispositivo, lo encuentra tal como lo dejó, ya sea en la web, en la app o en WhatsApp.
2. En el checkout, el cliente revisa los productos, ingresa la dirección de entrega, aplica un cupón si tiene uno y elige el método de pago.
3. El pedido se registra en estado **pendiente** (`PENDING`). Si el pago es con tarjeta, se confirma cuando Stripe aprueba el pago, por webhook o con la revisión automática que se hace cada 60 segundos. En ese momento se habilita el chat entre el cliente y el operador.
4. El operador **confirma** el pedido (`CONFIRMED`) y luego lo pasa a **en preparación** (`PREPARING`).
5. El operador asigna un domiciliario disponible y el pedido pasa a **en camino** (`ON_THE_WAY`). El domiciliario queda no disponible y el sistema genera un **PIN de entrega** de 4 dígitos.
6. El domiciliario inicia el trayecto y el cliente sigue su ubicación en el mapa. Al llegar, el cliente le da el PIN y el domiciliario lo ingresa en la app. Hay máximo 5 intentos; si se agotan, el PIN se bloquea y el operador resuelve la entrega con el cliente por el chat.
7. Con el PIN correcto, el pedido pasa a **entregado** (`DELIVERED`). Se guarda la fecha de entrega, se suman los puntos y el domiciliario vuelve a estar disponible.
8. Mientras el restaurante no lo haya confirmado, el cliente puede **cancelar** el pedido (`CANCELLED`). Si había pagado con tarjeta, se le reembolsa.

El cliente puede consultar en cualquier momento sus pedidos activos y su historial. El administrador consulta el historial general con filtros e indicadores, y puede exportarlo a PDF y Excel.

## Atención por WhatsApp

El cliente puede escribir al número del restaurante y recibe un menú interactivo con botones y listas:

- ver la carta y pedir recomendaciones, respondidas con inteligencia artificial;
- consultar horarios y ubicación;
- pedir que lo contacte un operador.

Si **vincula su cuenta** desde el enlace que le envía el asistente, además puede:

- agregar, cambiar o quitar productos de su carrito;
- consultar sus pedidos;
- consultar, crear, cambiar o cancelar su reserva.

Toda acción que modifica datos se confirma con un botón antes de ejecutarse. Las conversaciones se borran automáticamente según los plazos de la política de privacidad del sitio.

## Arquitectura

```
Web (Angular) ─┐
App (Android) ─┼──► API REST (Spring Boot) ──► PostgreSQL
WhatsApp ──────┘           │
                           └──► Stripe · Gemini · Google · Brevo · Meta WhatsApp
```

- Un solo backend atiende los tres canales con las mismas reglas de negocio.
- El backend usa dos conexiones a datos:
  - la **principal**, para usuarios, carta, pedidos, reservas, pagos y WhatsApp;
  - la del **módulo móvil**, para el carrito, el seguimiento de domicilios, la copia de pedidos y las acciones de carrito de WhatsApp.

  En desarrollo son dos bases distintas (H2 y PostgreSQL); en producción comparten la misma PostgreSQL de Railway.
- La autenticación usa **JWT** con roles. El frontend controla las páginas con guards y el backend valida el rol en cada petición.

La documentación completa de diseño (arquitectura, componentes, despliegue, modelo entidad-relación y diagramas de actividad) está en el documento **Documentación - TerraGalega**.

## Tecnologías

### Backend (`backend/demo`)

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 | Lenguaje |
| Spring Boot | 4.1.1 | Framework (Web MVC, Data JPA, Security, Mail, Thymeleaf, Actuator) |
| H2 | — | Base de datos principal en desarrollo |
| PostgreSQL | 16 | Base de datos del módulo móvil y base de producción |
| jjwt | 0.12.6 | Generación y validación de tokens JWT |
| springdoc-openapi | 3.1.1 | Documentación Swagger (solo en desarrollo) |
| stripe-java | 34.1.0 | Pagos con tarjeta, reembolsos y webhooks |
| google-genai | 1.54.0 | Asistente Galleguito y respuestas de WhatsApp (Gemini) |
| spring-security-oauth2-jose | — | Validación de tokens de Google Sign-In |
| OpenPDF | 1.3.43 | Facturas en PDF |
| ZXing | 3.5.3 | Código QR de las facturas |
| Lombok | — | Reducción de código repetitivo |
| Selenium + WebDriverManager | 5.4.1 | Pruebas de interfaz |

### Frontend (`frontend`)

| Tecnología | Versión | Uso |
|---|---|---|
| Angular | 19.2 | Framework (SPA con componentes standalone y lazy loading) |
| TypeScript | 5.7 | Lenguaje |
| RxJS | 7.8 | Programación reactiva |
| Tailwind CSS | 4.3 | Estilos |
| MapLibre GL | 6.19 | Mapas (con tiles de OpenFreeMap) |
| Chart.js + ng2-charts | 4.6 / 8.0 | Gráficas del panel de administración |
| jsPDF + jspdf-autotable | 4.2 / 5.0 | Reportes en PDF |
| ExcelJS | 4.4 | Reportes en Excel |
| driver.js | 1.14 | Tutoriales guiados |
| heic2any | 0.0.4 | Conversión de fotos HEIC en el chat del pedido |
| Angular CDK | 19.2 | Utilidades de accesibilidad e interfaz |

La web está disponible en **español, inglés y catalán**, con modo claro y oscuro y paletas para personas con daltonismo.

### Aplicación móvil (Android)

| Tecnología | Versión | Uso |
|---|---|---|
| Kotlin | 2.2.10 | Lenguaje |
| Jetpack Compose (BOM) | 2026.02.01 | Interfaz |
| Android Gradle Plugin | 9.5.1 | Compilación |
| Gradle | 9.6.0 | Herramienta de construcción |
| Android SDK | mínimo 24 (Android 7.0) · compileSdk/targetSdk 37 | Plataforma |
| Navigation Compose | 2.8.5 | Navegación entre pantallas |
| Retrofit + OkHttp | 2.11.0 / 4.12.0 | Consumo de la API REST |
| MapLibre Android SDK | 11.8.0 | Mapas y seguimiento del domicilio |
| Play Services Location | 21.3.0 | Ubicación del dispositivo |
| Credential Manager + Google ID | 1.6.0 / 1.2.0 | Inicio de sesión con Google |
| Coil | 2.7.0 | Carga de imágenes |
| AndroidX Browser | 1.4.0 | Pago con Stripe en Custom Tab |

### Servicios externos

| Servicio | Uso |
|---|---|
| Stripe | Pagos de pedidos, anticipos de reservas y suscripción Premium |
| Google Gemini | Inteligencia artificial de Galleguito y de WhatsApp |
| Google Sign-In / Geocoding | Inicio de sesión y conversión de direcciones a coordenadas |
| Meta WhatsApp Cloud API | Recepción y envío de mensajes de WhatsApp |
| Brevo | Envío de correos (verificación, recuperación de contraseña, facturas y reportes) |
| OpenFreeMap / OSRM | Mapas y cálculo de rutas |
| Railway | Despliegue del frontend, el backend y la base de datos |

## Estructura del repositorio

| Rama | Contenido |
|---|---|
| `backend` | API REST en Spring Boot (`demo/`), con `Dockerfile` y `docker-compose.yml` |
|
