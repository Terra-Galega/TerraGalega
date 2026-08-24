/* Formatea un número como precio en pesos colombianos */
const fmt = (price) =>
  new Intl.NumberFormat("es-CO", {
    style: "currency",
    currency: "COP",
    maximumFractionDigits: 0,
  }).format(price);

/* ─── Barra de navegación (según el Figma) ───
En /home la barra empieza transparente sobre la imagen del hero y se vuelve
"fija" (vidrio esmerilado claro) al hacer scroll. En el resto de páginas
(que no tienen imagen detrás) se queda siempre en el estado "fija" desde
que carga la página. El estado se controla agregando/quitando la clase
"barra-navegacion--fija", que ya trae todos los estilos definidos en
styles.css. */
(function initBarraNavegacion() {
  const barraNavegacion = document.getElementById("barra-navegacion");
  if (!barraNavegacion) return;
  const esInicio = barraNavegacion.dataset.inicio === "true";

  function actualizarBarraNavegacion() {
    const fija = !esInicio || window.scrollY > 20;
    barraNavegacion.classList.toggle("barra-navegacion--fija", fija);
  }

  if (esInicio) {
    window.addEventListener("scroll", actualizarBarraNavegacion, {
      passive: true,
    });
  }
  actualizarBarraNavegacion();
})();

/*Variables para gestionar el carrito de compras */
let cart = [];
let categoriaActual = "Todos";
let itemSeleccionado = null;
let qty = 1;
let adicionalesSeleccionados = [];
/* Elementos del DOM para gestionar el carrito */
const cuadriculaMenu = document.getElementById("cuadricula-menu");
const pestanasCategoria = document.getElementById("pestanas-categoria");
const carritoContadorEscritorio = document.getElementById(
  "carrito-contador-escritorio",
);
const carritoContadorMovil = document.getElementById("carrito-contador-movil");
const botonMenuMovil = document.getElementById("boton-menu-movil");
const menuMovil = document.getElementById("menu-movil");
const iconoAbrir = document.getElementById("icono-abrir");
const iconoCerrar = document.getElementById("icono-cerrar");

/* Función para abrir el modal de un producto específico mediante su ID enviado desde Thymeleaf */
function abrirProductoPorId(id) {
  const item = productosMenu.find((p) => p.id === id);
  if (item) {
    abrirProducto(item);
  }
}

/* Elementos del DOM de la página /menu (buscador y estado vacío) */
const buscadorMenuInput = document.getElementById("buscador-menu");
const menuVacioEl = document.getElementById("menu-vacio");
const botonReiniciarMenu = document.getElementById("reiniciar-menu");
let busquedaActual = "";

/* Filtra los elementos del DOM renderizados por Thymeleaf según la categoría
y el texto de búsqueda actuales, y muestra el mensaje de "sin resultados" si aplica */
function filtrarItemsMenu() {
  const items = document.querySelectorAll(".item-menu");
  let visibleCount = 0;
  items.forEach((item) => {
    const matchesCategory =
      categoriaActual === "Todos" || item.dataset.category === categoriaActual;
    const matchesSearch = item.dataset.name
      .toLowerCase()
      .includes(busquedaActual.toLowerCase());
    if (matchesCategory && matchesSearch) {
      item.style.display = "block";
      visibleCount++;
    } else {
      item.style.display = "none";
    }
  });
  if (menuVacioEl) {
    menuVacioEl.classList.toggle("hidden-modal", visibleCount !== 0);
  }
}

/* Maneja el cambio de categoría al hacer clic en los botones de categoría */
/* Se protege con un if porque no todas las páginas tienen la grilla completa del menú (p. ej. el home solo muestra "Favoritos") */
if (pestanasCategoria) {
  pestanasCategoria.querySelectorAll(".btn-categoria").forEach((btn) => {
    btn.addEventListener("click", () => {
      /* Actualiza la categoría actual eliminando la clase "active" de 
      todos los botones y agregando la clase a aquel que fue CLICK */
      categoriaActual = btn.dataset.category;
      pestanasCategoria
        .querySelectorAll(".btn-categoria")
        .forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");
      filtrarItemsMenu();
    });
  });
}

/* Maneja la búsqueda de platos por nombre en la página /menu */
if (buscadorMenuInput) {
  buscadorMenuInput.addEventListener("input", () => {
    busquedaActual = buscadorMenuInput.value.trim();
    filtrarItemsMenu();
  });
}

/* Botón para limpiar la búsqueda y la categoría cuando no hay resultados */
if (botonReiniciarMenu) {
  botonReiniciarMenu.addEventListener("click", () => {
    busquedaActual = "";
    if (buscadorMenuInput) buscadorMenuInput.value = "";
    categoriaActual = "Todos";
    if (pestanasCategoria) {
      pestanasCategoria
        .querySelectorAll(".btn-categoria")
        .forEach((b) => b.classList.remove("active"));
      pestanasCategoria
        .querySelector('[data-category="Todos"]')
        ?.classList.add("active");
    }
    filtrarItemsMenu();
  });
}

/* Maneja el scroll suave al hacer clic en los enlaces de navegación */
document.querySelectorAll("[data-scroll]").forEach((el) => {
  el.addEventListener("click", () => {
    const id = el.dataset.scroll;
    /* Hace scroll suave hacia el elemento con el ID correspondiente */
    document.getElementById(id)?.scrollIntoView({ behavior: "smooth" });
    /* Cierra el menú móvil si está abierto */
    cerrarMenuMovil();
  });
});

/* Maneja la apertura y cierre del menú móvil al hacer clic en el botón de navegación */
botonMenuMovil.addEventListener("click", () => {
  const isOpen = !menuMovil.classList.contains("hidden-modal");
  /* Si el menú móvil está abierto, lo cierra; de lo contrario, lo abre y ajusta los íconos */
  if (isOpen) {
    cerrarMenuMovil();
  } else {
    /* Abre el menú móvil y ajusta los íconos */
    menuMovil.classList.remove("hidden-modal");
    iconoAbrir.classList.add("hidden-modal");
    iconoCerrar.classList.remove("hidden-modal");
  }
});
/* Función para cerrar el menú móvil y ajustar los íconos */
function cerrarMenuMovil() {
  menuMovil.classList.add("hidden-modal");
  iconoAbrir.classList.remove("hidden-modal");
  iconoCerrar.classList.add("hidden-modal");
}
/* Maneja la apertura y cierre de los modales (cart, product) */
/* Los modales de login/signup pasaron a ser la página /login, igual que en el Figma */
/*modales=div que se esconden a menos que se abran con un click */
const modales = {
  cart: document.getElementById("modal-carrito"),
  product: document.getElementById("modal-producto"),
};
/* Función para abrir un modal */
function abrirModal(name) {
  cerrarTodosModales();
  modales[name]?.classList.remove("hidden-modal");
}
/* Función para cerrar todos los modales */
function cerrarTodosModales() {
  Object.values(modales).forEach((m) => m?.classList.add("hidden-modal"));
}
/* Agrega event listeners a los elementos con el atributo data-modal 
para abrir el modal correspondiente al hacer clic */
document.querySelectorAll("[data-modal]").forEach((el) => {
  el.addEventListener("click", () => {
    /* Abre el modal correspondiente al valor del atributo data-modal */
    abrirModal(el.dataset.modal);
    /* Cierra el menú móvil si está abierto */
    cerrarMenuMovil();
  });
});

/* Agrega event listeners a los elementos con el atributo data-close
 para cerrar todos los modales al hacer clic */
/* la x para cerrar la pestaña */
document.querySelectorAll("[data-close]").forEach((el) => {
  el.addEventListener("click", () => cerrarTodosModales());
});

/* Agrega event listeners a los modales para cerrar el modal al 
hacer clic fuera del contenido */
Object.values(modales).forEach((modal) => {
  modal.addEventListener("click", (e) => {
    if (e.target === modal) cerrarTodosModales();
  });
});

/* Agrega un event listener al botón "Explorar Menú" en el 
carrito para cerrar todos los modales y hacer scroll hacia la sección del menú */
document
  .getElementById("carrito-explorar-menu")
  .addEventListener("click", () => {
    cerrarTodosModales();
    document.getElementById("menu")?.scrollIntoView({ behavior: "smooth" });
  });

/* Función para abrir el modal de un producto específico */
function abrirProducto(item) {
  itemSeleccionado = item;
  qty = 1;
  adicionalesSeleccionados = [];
  renderizarModalProducto();
  abrirModal("product");
}
/* Función para renderizar el contenido del modal de producto */
function renderizarModalProducto() {
  if (!itemSeleccionado) return;
  /* Actualiza el contenido del modal con la información del producto seleccionado */
  /* Adaptado para consumir 'imageUrl' que proviene de Spring Boot */
  document.getElementById("producto-imagen").src = itemSeleccionado.imageUrl;
  document.getElementById("producto-imagen").alt = itemSeleccionado.name;
  document.getElementById("producto-nombre").textContent =
    itemSeleccionado.name;
  document.getElementById("producto-precio").textContent = fmt(
    itemSeleccionado.price,
  );
  /* Actualiza la descripción del producto y la cantidad seleccionada */
  document.getElementById("producto-descripcion").textContent =
    itemSeleccionado.description;
  document.getElementById("cantidad-valor").textContent = qty;
  /* Actualiza la sección de adicionales del producto */
  const addsWrap = document.getElementById("producto-adicionales-wrap");
  const addsContainer = document.getElementById("producto-adicionales");
  /* Si el producto no tiene adicionales, oculta la sección
  de lo contrario, muestra los adicionales disponibles */
  if (itemSeleccionado.additionals.length === 0) {
    addsWrap.classList.add("hidden-modal");
  } else {
    /* Muestra la sección de adicionales y genera los checkboxes para cada adicional */
    /* Adaptado para consumir 'add.name' de la entidad AddOn de Java */
    addsWrap.classList.remove("hidden-modal");
    addsContainer.innerHTML = itemSeleccionado.additionals
      .map(
        (add, i) => `
      <label class="flex items-center gap-3 cursor-pointer select-none">
        <input type="checkbox" data-add="${i}" ${adicionalesSeleccionados.includes(add.name) ? "checked" : ""} style="accent-color:#b2571f; width:16px; height:16px; flex-shrink:0;" />
        <span class="text-sm" style="color: rgba(44,44,44,0.7);">${add.name}</span>
      </label>
    `,
      )
      .join("");
    /* Agrega event listeners a los checkboxes de adicionales para 
      actualizar la lista de adicionales seleccionados */
    addsContainer.querySelectorAll("input[type=checkbox]").forEach((cb, i) => {
      cb.addEventListener("change", () => {
        /* Si el adicional ya está seleccionado, lo elimina de la lista
          de lo contrario, lo agrega a la lista */
        const add = itemSeleccionado.additionals[i];
        if (adicionalesSeleccionados.includes(add.name)) {
          adicionalesSeleccionados = adicionalesSeleccionados.filter(
            (a) => a !== add.name,
          );
        } else {
          adicionalesSeleccionados.push(add.name);
        }
      });
    });
  }
  /* Actualiza el texto del botón "Agregar al carrito" 
con el precio total según la cantidad seleccionada */
  document.getElementById("btn-agregar-carrito").textContent =
    `Agregar al carrito — ${fmt(itemSeleccionado.price * qty)}`;
}

/* Agrega event listeners a los botones de cantidad para aumentar o
 disminuir la cantidad seleccionada */
/* Agrega event listener al botón de disminuir cantidad */
document.getElementById("cantidad-menos").addEventListener("click", () => {
  qty = Math.max(1, qty - 1);
  renderizarModalProducto();
});
/* Agrega event listener al botón de aumentar cantidad */
document.getElementById("cantidad-mas").addEventListener("click", () => {
  qty = qty + 1;
  renderizarModalProducto();
});
/* Agrega event listener al botón "Agregar al carrito" para agregar 
el producto seleccionado al carrito */
document.getElementById("btn-agregar-carrito").addEventListener("click", () => {
  if (!itemSeleccionado) return;
  /* Verifica si el producto ya está en el carrito y actualiza la cantidad
  de lo contrario, agrega el producto al carrito */
  const existente = cart.find((c) => c.id === itemSeleccionado.id);
  if (existente) {
    existente.quantity += qty;
  } else {
    /* Agrega el producto seleccionado al carrito con la cantidad especificada */
    cart.push({ ...itemSeleccionado, quantity: qty });
  }
  /* Renderiza el carrito actualizado y cierra todos los modales */
  renderizarCarrito();
  cerrarTodosModales();
});

/* Función para renderizar el contenido del carrito de compras 
ACTUALIZAR CARRITO*/
function renderizarCarrito() {
  const count = cart.reduce((s, i) => s + i.quantity, 0);
  const total = cart.reduce((s, i) => s + i.price * i.quantity, 0);
  /* Actualiza el contador de elementos en el carrito en la interfaz de usuario */
  [carritoContadorEscritorio, carritoContadorMovil].forEach((el) => {
    if (count > 0) {
      /* Si hay elementos en el carrito, muestra el contador y actualiza su contenido */
      el.textContent = count;
      el.classList.remove("hidden-modal");
    } else {
      el.classList.add("hidden-modal");
    }
  });
  /* Obtiene los elementos del DOM para mostrar los elementos del carrito, 
el mensaje de carrito vacío y el pie de página del carrito */
  const carritoItemsEl = document.getElementById("carrito-items");
  const carritoVacioEl = document.getElementById("carrito-vacio");
  const carritoPieEl = document.getElementById("carrito-pie");
  /* Si el carrito está vacío, muestra el mensaje de carrito vacío y oculta los
   elementos del carrito y el pie de página */
  if (cart.length === 0) {
    carritoVacioEl.classList.remove("hidden-modal");
    carritoItemsEl.innerHTML = "";
    carritoPieEl.classList.add("hidden-modal");
    return;
  }
  /* Si el carrito tiene elementos, oculta el mensaje de carrito vacío,
   muestra los elementos del carrito y el pie de página, y actualiza el total */
  carritoVacioEl.classList.add("hidden-modal");
  carritoPieEl.classList.remove("hidden-modal");
  document.getElementById("carrito-total").textContent = fmt(total);
  /* Genera el HTML para cada elemento del carrito y lo inserta en el contenedor */
  /* Adaptado para consumir 'imageUrl' que proviene de Spring Boot */
  carritoItemsEl.innerHTML = cart
    .map(
      (item) => `
    <div class="flex items-center gap-4 bg-white p-3" style="border-radius:2px;">
      <img src="${item.imageUrl}" alt="${item.name}" class="w-16 h-16 object-cover flex-shrink-0" style="border-radius:2px;" />
      <div class="flex-1 min-w-0">
        <p class="font-medium text-charcoal text-sm truncate">${item.name}</p>
        <p class="text-sm font-bold" style="color:#b2571f;">${fmt(item.price)}</p>
        <p class="text-xs" style="color: rgba(44,44,44,0.4);">× ${item.quantity}</p>
      </div>
      <button class="eliminar-item flex-shrink-0" style="color: rgba(44,44,44,0.3);" data-id="${item.id}" aria-label="Eliminar">
        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
        </svg>
      </button>
    </div>
  `,
    )
    .join("");
  /* Agrega event listeners a los botones de eliminar elementos del carrito 
    para eliminar el elemento correspondiente al hacer clic */
  carritoItemsEl.querySelectorAll(".eliminar-item").forEach((btn) => {
    btn.addEventListener("click", () => {
      cart = cart.filter((c) => c.id !== Number(btn.dataset.id));
      renderizarCarrito();
    });
  });
}
/* Agrega un event listener al botón "Finalizar Compra" en el carrito
 para llevar a la página de inicio de sesión (@{/login}, vía data-href) */
const botonFinalizarCompra = document.getElementById("carrito-finalizar");
if (botonFinalizarCompra) {
  botonFinalizarCompra.addEventListener("click", () => {
    window.location.href = botonFinalizarCompra.dataset.href;
  });
}

/* A partir de aquí: lógica exclusiva de la página /login (pestañas, accesos
de prueba y envío de los formularios). Se protege todo con "if" porque
formulario-login/formulario-registro solo existen en esa página. */
const formularioLogin = document.getElementById("formulario-login");
const formularioRegistro = document.getElementById("formulario-registro");

/* Pestañas "Iniciar sesión" / "Registrarse" */
const pestanasSesion = document.getElementById("pestanas-sesion");
const subtituloSesion = document.getElementById("subtitulo-sesion");

/* Cambia a la pestaña indicada ("login" | "signup") */
function establecerPestanaSesion(tab) {
  if (!pestanasSesion || !formularioLogin || !formularioRegistro) return;
  const btn = pestanasSesion.querySelector(`[data-tab="${tab}"]`);
  if (!btn) return;
  pestanasSesion
    .querySelectorAll(".pestana-sesion")
    .forEach((b) => b.classList.remove("active"));
  btn.classList.add("active");
  const isLogin = tab === "login";
  formularioLogin.classList.toggle("hidden-modal", !isLogin);
  formularioRegistro.classList.toggle("hidden-modal", isLogin);
  if (subtituloSesion) {
    subtituloSesion.textContent = isLogin
      ? "Bienvenido de vuelta"
      : "Únete a nosotros";
  }
}

if (pestanasSesion && formularioLogin && formularioRegistro) {
  pestanasSesion.querySelectorAll(".pestana-sesion").forEach((btn) => {
    btn.addEventListener("click", () =>
      establecerPestanaSesion(btn.dataset.tab),
    );
  });

  /* Si se llega desde el barra-navegacion con "Registrarse" (@{/login(tab='register')}),
  abre directamente la pestaña de registro en lugar de la de inicio de sesión */
  const requestedTab = new URLSearchParams(window.location.search).get("tab");
  if (requestedTab === "register") {
    establecerPestanaSesion("signup");
  }
}

/* Usuarios de prueba (mismos datos que se muestran en los accesos rápidos)
usados para reconocer el rol de la persona que inicia sesión y decidir
a dónde redirigirla (cliente -> inicio, admin -> panel de administración) */
const USUARIOS_DEMO = {
  "admin@terra.com": {
    password: "admin123",
    name: "Administrador",
    role: "admin",
  },
  "cliente@terra.com": {
    password: "cliente123",
    name: "María García",
    role: "cliente",
  },
};

/* Guarda la sesión activa en el navegador (localStorage) para que la
página /admin pueda comprobar si quien la visita inició sesión como admin */
function guardarSesion(sesion) {
  localStorage.setItem("usuarioTerraGalega", JSON.stringify(sesion));
}

/* Accesos de prueba: solo rellenan el formulario, no autentican contra un backend real */
document.querySelectorAll(".btn-acceso-demo").forEach((btn) => {
  btn.addEventListener("click", () => {
    document.getElementById("login-correo").value = btn.dataset.email;
    document.getElementById("login-contrasena").value = btn.dataset.password;
  });
});

/* Agrega event listeners a los formularios de inicio de sesión y registro 
para manejar la validación y el envío de datos */
if (formularioLogin) {
  formularioLogin.addEventListener("submit", (e) => {
    e.preventDefault();
    /* Obtiene los valores de correo electrónico y contraseña del formulario de inicio de sesión */
    const email = document.getElementById("login-correo").value.trim();
    const password = document.getElementById("login-contrasena").value.trim();
    const errorEl = document.getElementById("login-error");
    /* Valida que ambos campos estén completos; si no, muestra un mensaje de error */
    if (!email || !password) {
      errorEl.textContent = "Por favor completa todos los campos.";
      errorEl.classList.remove("hidden-modal");
      return;
    }

    /* Comprueba si el correo/contraseña corresponden a uno de los usuarios de
    prueba para saber si quien inicia sesión es un administrador */
    const usuarioDemo = USUARIOS_DEMO[email.toLowerCase()];
    if (usuarioDemo && usuarioDemo.password !== password) {
      errorEl.textContent = "Contraseña incorrecta.";
      errorEl.classList.remove("hidden-modal");
      return;
    }

    /* Si la validación es exitosa, oculta el mensaje de error, guarda la sesión 
    y redirige según el rol: administradores al panel /admin, clientes al inicio */
    errorEl.classList.add("hidden-modal");
    const sesion = usuarioDemo
      ? { name: usuarioDemo.name, email, role: usuarioDemo.role }
      : { name: email.split("@")[0], email, role: "cliente" };
    guardarSesion(sesion);
    formularioLogin.reset();
    if (sesion.role === "admin") {
      window.location.href = "/admin";
    } else {
      alert("Bienvenido de nuevo a Terra Galega");
      window.location.href = "/";
    }
  });
}
/* Agrega un event listener al formulario de registro para manejar la validación y el envío de datos */
if (formularioRegistro) {
  formularioRegistro.addEventListener("submit", (e) => {
    e.preventDefault();
    /* Obtiene los valores de nombre, apellido, correo electrónico, contraseña,
     teléfono y dirección del formulario de registro */
    const name = document.getElementById("registro-nombre").value.trim();
    const lastName = document.getElementById("registro-apellido").value.trim();
    const email = document.getElementById("registro-correo").value.trim();
    const password = document
      .getElementById("registro-contrasena")
      .value.trim();
    const phone = document.getElementById("registro-telefono").value.trim();
    const address = document.getElementById("registro-direccion").value.trim();
    const errorEl = document.getElementById("registro-error");
    /* Valida que todos los campos estén completos; si no, muestra un mensaje de error */
    if (!name || !lastName || !email || !password || !phone || !address) {
      errorEl.textContent = "Por favor completa todos los campos.";
      errorEl.classList.remove("hidden-modal");
      return;
    }
    /* Si la validación es exitosa, oculta el mensaje de error, guarda la sesión
    (rol "cliente"), muestra un mensaje de bienvenida y regresa al inicio */
    errorEl.classList.add("hidden-modal");
    guardarSesion({ name, email, role: "cliente" });
    alert(
      `Bienvenido a Terra Galega, ${name}. Tu cuenta fue creada exitosamente.`,
    );
    formularioRegistro.reset();
    window.location.href = "/";
  });
}
/* Agrega un event listener al formulario de contacto (página /contacto) para 
manejar la validación y el envío de datos, igual que en login/registro */
const formularioContacto = document.getElementById("formulario-contacto");
if (formularioContacto) {
  formularioContacto.addEventListener("submit", (e) => {
    e.preventDefault();
    const name = document.getElementById("contacto-nombre").value.trim();
    const email = document.getElementById("contacto-correo").value.trim();
    const message = document.getElementById("contacto-mensaje").value.trim();
    const errorEl = document.getElementById("contacto-error");
    /* Valida que todos los campos estén completos; si no, muestra un mensaje de error */
    if (!name || !email || !message) {
      errorEl.textContent = "Por favor completa todos los campos.";
      errorEl.classList.remove("hidden-modal");
      return;
    }
    errorEl.classList.add("hidden-modal");
    alert(
      `Gracias ${name}, hemos recibido tu mensaje. Te contactaremos pronto.`,
    );
    formularioContacto.reset();
  });
}

/* Renderiza los testimonios de clientes en la sección correspondiente */
const TESTIMONIOS = [
  /* Cada testimonio contiene el nombre del cliente, su rol y el texto del testimonio */
  {
    name: "María García",
    role: "Cliente fiel",
    text: "El pulpo a la gallega es simplemente extraordinario. Me transporta directamente a Galicia con cada bocado. El ambiente cálido y el servicio impecable hacen de cada visita una experiencia única.",
  },
  {
    name: "Carlos Rodríguez",
    role: "Crítico gastronómico",
    text: "Terra Galega es la joya gastronómica de la ciudad. La paella de mariscos es perfecta en sabor y textura. Ingredientes de primera calidad con técnica y pasión en cada preparación.",
  },
  {
    name: "Lucía Martínez",
    role: "Food blogger",
    text: "Desde las croquetas hasta la tarta de Santiago, cada plato cuenta la historia de Galicia. El servicio es cálido, la ambientación preciosa y los sabores completamente auténticos.",
  },
];
/* Función para renderizar los testimonios en la sección de la página */
function renderizarTestimonios() {
  const grid = document.getElementById("cuadricula-testimonios");
  if (!grid) return;
  /* Genera el HTML para cada testimonio y lo inserta en el contenedor */
  const star = `<svg class="w-4 h-4" fill="#d99a21" viewBox="0 0 20 20"><path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/></svg>`;
  grid.innerHTML = TESTIMONIOS.map(
    (t) => `
    <div class="bg-cream p-8 shadow-sm" style="border-radius:2px;">
      <div class="flex gap-1 mb-5">${star.repeat(5)}</div>
      <p class="text-sm leading-relaxed mb-6 italic" style="color: rgba(44,44,44,0.68);">"${t.text}"</p>
      <div class="pt-5 border-t" style="border-color: rgba(44,44,44,0.1);">
        <p class="font-semibold text-charcoal text-sm">${t.name}</p>
        <p class="text-xs mt-0.5" style="color: rgba(44,44,44,0.45);">${t.role}</p>
      </div>
    </div>
  `,
  ).join("");
}

/* Renderiza el carrito y los testimonios al cargar la página */
renderizarCarrito();
renderizarTestimonios();
