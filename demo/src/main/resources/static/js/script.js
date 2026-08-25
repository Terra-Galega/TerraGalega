/* Formatea un número como precio en pesos colombianos */
const fmt = (price) =>
  new Intl.NumberFormat("es-CO", {
    style: "currency",
    currency: "COP",
    maximumFractionDigits: 0,
  }).format(price);

/* Barra de navegación */
(function initNavBar() {
  const navBar = document.getElementById("nav-bar");
  if (!navBar) return;
  const isHome = navBar.dataset.inicio === "true";

  function updateNavBar() {
    const stayed = !isHome || window.scrollY > 20;
    navBar.classList.toggle("nav-bar--stayed", stayed);
  }

  if (isHome) {
    window.addEventListener("scroll", updateNavBar, { passive: true });
  }
  updateNavBar();
})();

/* Variables para gestionar el carrito de compras */
let cart = [];
let currentCategory = "Todos";
let selectedItem = null;
let qty = 1;
let selectedAdditionals = [];

/* Elementos del DOM para gestionar el carrito y el filtro del menú */
const menuGrid = document.getElementById("menu-grid");
const categoryTabs = document.getElementById("category-tabs");
const cartCountDesktop = document.getElementById("cart-count-desktop");
const cartCountMobile = document.getElementById("cart-count-mobile");
const mobileMenuBtn = document.getElementById("mobile-menu-btn");
const mobileMenu = document.getElementById("mobile-menu");
const iconOpen = document.getElementById("icon-open");
const iconClose = document.getElementById("icon-close");

/* Arreglo de productos que viene desde Thymeleaf (declarado inline en cada
página con th:inline="javascript" como "const menuProducts = [...]") */
const productList = typeof menuProducts !== "undefined" ? menuProducts : [];

/* Función para abrir el modal de un producto específico mediante su ID enviado desde Thymeleaf */
function openProductById(id) {
  const item = productList.find((p) => p.id === id);
  if (item) {
    openProduct(item);
  }
}

/* Elementos del DOM de la página /menu (buscador y estado vacío) */
const menuSearchInput = document.getElementById("menu-search");
const menuEmptyEl = document.getElementById("menu-empty");
const menuResetBtn = document.getElementById("menu-reset");
let currentSearch = "";

/* Filtra los elementos del DOM renderizados por Thymeleaf según la categoría
y el texto de búsqueda actuales, y muestra el mensaje de "sin resultados" si aplica */
function filterMenuItems() {
  const items = document.querySelectorAll(".menu-item");
  let visibleCount = 0;
  items.forEach((item) => {
    const matchesCategory =
      currentCategory === "Todos" || item.dataset.category === currentCategory;
    const matchesSearch = (item.dataset.name || "")
      .toLowerCase()
      .includes(currentSearch.toLowerCase());
    if (matchesCategory && matchesSearch) {
      item.style.display = "block";
      visibleCount++;
    } else {
      item.style.display = "none";
    }
  });
  if (menuEmptyEl) {
    menuEmptyEl.classList.toggle("hidden-modal", visibleCount !== 0);
  }
}

/* Maneja el cambio de categoría al hacer clic en los botones de categoría */
/* Se protege con un if porque no todas las páginas tienen la grilla completa del menú (p. ej. el home solo muestra "Favoritos") */
if (categoryTabs) {
  categoryTabs.querySelectorAll(".category-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      /* Actualiza la categoría actual eliminando la clase "active" de 
      todos los botones y agregando la clase a aquel que fue CLICK */
      currentCategory = btn.dataset.category;
      categoryTabs
        .querySelectorAll(".category-btn")
        .forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");
      filterMenuItems();
    });
  });
}

/* Maneja la búsqueda de platos por nombre en la página /menu */
if (menuSearchInput) {
  menuSearchInput.addEventListener("input", () => {
    currentSearch = menuSearchInput.value.trim();
    filterMenuItems();
  });
}

/* Botón para limpiar la búsqueda y la categoría cuando no hay resultados */
if (menuResetBtn) {
  menuResetBtn.addEventListener("click", () => {
    currentSearch = "";
    if (menuSearchInput) menuSearchInput.value = "";
    currentCategory = "Todos";
    if (categoryTabs) {
      categoryTabs
        .querySelectorAll(".category-btn")
        .forEach((b) => b.classList.remove("active"));
      categoryTabs
        .querySelector('[data-category="Todos"]')
        ?.classList.add("active");
    }
    filterMenuItems();
  });
}

/* Maneja el scroll suave al hacer clic en los enlaces de navegación */
document.querySelectorAll("[data-scroll]").forEach((el) => {
  el.addEventListener("click", () => {
    const id = el.dataset.scroll;
    /* Hace scroll suave hacia el elemento con el ID correspondiente */
    document.getElementById(id)?.scrollIntoView({ behavior: "smooth" });
    /* Cierra el menú móvil si está abierto */
    closeMobileMenu();
  });
});


if (mobileMenuBtn && mobileMenu) {
  mobileMenuBtn.addEventListener("click", () => {
    const isOpen = !mobileMenu.classList.contains("hidden-modal");
    /* Si el menú móvil está abierto, lo cierra; de lo contrario, lo abre y ajusta los íconos */
    if (isOpen) {
      closeMobileMenu();
    } else {
      /* Abre el menú móvil y ajusta los íconos */
      mobileMenu.classList.remove("hidden-modal");
      iconOpen?.classList.add("hidden-modal");
      iconClose?.classList.remove("hidden-modal");
    }
  });
}
/* Función para cerrar el menú móvil y ajustar los íconos */
function closeMobileMenu() {
  mobileMenu?.classList.add("hidden-modal");
  iconOpen?.classList.remove("hidden-modal");
  iconClose?.classList.add("hidden-modal");
}
/*apertura y cierre de los modales (cart, product) */
/* Los modales de login/signup pasaron a ser la página /login, igual que en el Figma */
/*modales=div que se esconden a menos que se abran con un click */
const modals = {
  cart: document.getElementById("modal-cart"),
  product: document.getElementById("modal-product"),
};
/* Función para abrir un modal */
function openModal(name) {
  closeAllModals();
  modals[name]?.classList.remove("hidden-modal");
}
/* Función para cerrar todos los modales */
function closeAllModals() {
  Object.values(modals).forEach((m) => m?.classList.add("hidden-modal"));
}
/* Agrega event listeners a los elementos con el atributo data-modal 
para abrir el modal correspondiente al hacer clic */
document.querySelectorAll("[data-modal]").forEach((el) => {
  el.addEventListener("click", () => {
    /* Abre el modal correspondiente al valor del atributo data-modal */
    openModal(el.dataset.modal);
    /* Cierra el menú móvil si está abierto */
    closeMobileMenu();
  });
});

/* Agrega event listeners a los elementos con el atributo data-close
 para cerrar todos los modales al hacer clic */
/* la x para cerrar la pestaña */
document.querySelectorAll("[data-close]").forEach((el) => {
  el.addEventListener("click", () => closeAllModals());
});

/* Agrega event listeners a los modales para cerrar el modal al 
hacer clic fuera del contenido. */
Object.values(modals)
  .filter(Boolean)
  .forEach((modal) => {
    modal.addEventListener("click", (e) => {
      if (e.target === modal) closeAllModals();
    });
  });

/* Agrega un event listener al botón "Explorar Menú" en el 
carrito para cerrar todos los modales y hacer scroll hacia la sección del menú */
document
  .getElementById("cart-explore-menu")
  ?.addEventListener("click", () => {
    closeAllModals();
    document.getElementById("menu")?.scrollIntoView({ behavior: "smooth" });
  });

/* Función para abrir el modal de un producto específico */
function openProduct(item) {
  selectedItem = item;
  qty = 1;
  selectedAdditionals = [];
  renderProductModal();
  openModal("product");
}
/* Función para renderizar el contenido del modal de producto. */
function renderProductModal() {
  if (!selectedItem) return;

  const imageEl = document.getElementById("product-image");
  const nameEl = document.getElementById("product-name");
  const priceEl = document.getElementById("product-price");
  const descriptionEl = document.getElementById("product-description");
  const qtyValueEl = document.getElementById("qty-value");
  const addsWrap = document.getElementById("product-additionals-wrap");
  const addsContainer = document.getElementById("product-additionals");
  const addToCartBtn = document.getElementById("add-to-cart-btn");

  /* Actualiza el contenido del modal con la información del producto seleccionado */
  /* Adaptado para consumir 'imageUrl' que proviene de Spring Boot */
  if (imageEl) {
    imageEl.src = selectedItem.imageUrl;
    imageEl.alt = selectedItem.name;
  }
  if (nameEl) nameEl.textContent = selectedItem.name;
  if (priceEl) priceEl.textContent = fmt(selectedItem.price);
  /* Actualiza la descripción del producto y la cantidad seleccionada */
  if (descriptionEl) descriptionEl.textContent = selectedItem.description;
  if (qtyValueEl) qtyValueEl.textContent = qty;

  /* Actualiza la sección de adicionales del producto */
  if (addsWrap && addsContainer) {
    /* Si el producto no tiene adicionales, oculta la sección
    de lo contrario, muestra los adicionales disponibles */
    if (selectedItem.additionals.length === 0) {
      addsWrap.classList.add("hidden-modal");
    } else {
      /* Muestra la sección de adicionales y genera los checkboxes para cada adicional */
      /* Adaptado para consumir 'add.name' de la entidad AddOn de Java */
      addsWrap.classList.remove("hidden-modal");
      addsContainer.innerHTML = selectedItem.additionals
        .map(
          (add, i) => `
        <label class="flex items-center gap-3 cursor-pointer select-none">
          <input type="checkbox" data-add="${i}" ${selectedAdditionals.includes(add.name) ? "checked" : ""} style="accent-color:#b2571f; width:16px; height:16px; flex-shrink:0;" />
          <span class="text-sm" style="color: rgba(44,44,44,0.7);">${add.name}</span>
        </label>
      `,
        )
        .join("");
      /* Agrega event listeners a los checkboxes de adicionales para 
        actualizar la lista de adicionales seleccionados */
      addsContainer
        .querySelectorAll("input[type=checkbox]")
        .forEach((cb, i) => {
          cb.addEventListener("change", () => {
            /* Si el adicional ya está seleccionado, lo elimina de la lista
            de lo contrario, lo agrega a la lista */
            const add = selectedItem.additionals[i];
            if (selectedAdditionals.includes(add.name)) {
              selectedAdditionals = selectedAdditionals.filter(
                (a) => a !== add.name,
              );
            } else {
              selectedAdditionals.push(add.name);
            }
          });
        });
    }
  }
  /* Actualiza el texto del botón "Agregar al carrito" 
con el precio total según la cantidad seleccionada */
  if (addToCartBtn) {
    addToCartBtn.textContent = `Agregar al carrito — ${fmt(selectedItem.price * qty)}`;
  }
}

/* Agrega event listener al botón de disminuir cantidad */
document.getElementById("qty-minus")?.addEventListener("click", () => {
  qty = Math.max(1, qty - 1);
  renderProductModal();
});
/* Agrega event listener al botón de aumentar cantidad */
document.getElementById("qty-plus")?.addEventListener("click", () => {
  qty = qty + 1;
  renderProductModal();
});
/* Agrega event listener al botón "Agregar al carrito" para agregar 
el producto seleccionado al carrito */
document.getElementById("add-to-cart-btn")?.addEventListener("click", () => {
  if (!selectedItem) return;
  /* Verifica si el producto ya está en el carrito y actualiza la cantidad
    de lo contrario, agrega el producto al carrito */
  const existing = cart.find((c) => c.id === selectedItem.id);
  if (existing) {
    existing.quantity += qty;
  } else {
    /* Agrega el producto seleccionado al carrito con la cantidad especificada */
    cart.push({ ...selectedItem, quantity: qty });
  }
  /* Renderiza el carrito actualizado y cierra todos los modales */
  renderCart();
  closeAllModals();
});

/* Función para renderizar el contenido del carrito de compras */
function renderCart() {
  const count = cart.reduce((s, i) => s + i.quantity, 0);
  const total = cart.reduce((s, i) => s + i.price * i.quantity, 0);
  /* Actualiza el contador de elementos en el carrito en la interfaz de usuario */
  [cartCountDesktop, cartCountMobile].filter(Boolean).forEach((el) => {
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
  const cartItemsEl = document.getElementById("cart-items");
  const cartEmptyEl = document.getElementById("cart-empty");
  const cartFooterEl = document.getElementById("cart-footer");
  const cartTotalEl = document.getElementById("cart-total");
  if (!cartItemsEl || !cartEmptyEl || !cartFooterEl) return;
  /* Si el carrito está vacío, muestra el mensaje de carrito vacío y oculta los
   elementos del carrito y el pie de página */
  if (cart.length === 0) {
    cartEmptyEl.classList.remove("hidden-modal");
    cartItemsEl.innerHTML = "";
    cartFooterEl.classList.add("hidden-modal");
    return;
  }
  /* Si el carrito tiene elementos, oculta el mensaje de carrito vacío,
   muestra los elementos del carrito y el pie de página, y actualiza el total */
  cartEmptyEl.classList.add("hidden-modal");
  cartFooterEl.classList.remove("hidden-modal");
  if (cartTotalEl) cartTotalEl.textContent = fmt(total);
  /* Genera el HTML para cada elemento del carrito y lo inserta en el contenedor */
  /* Adaptado para consumir 'imageUrl' que proviene de Spring Boot */
  cartItemsEl.innerHTML = cart
    .map(
      (item) => `
    <div class="flex items-center gap-4 bg-white p-3" style="border-radius:2px;">
      <img src="${item.imageUrl}" alt="${item.name}" class="w-16 h-16 object-cover flex-shrink-0" style="border-radius:2px;" />
      <div class="flex-1 min-w-0">
        <p class="font-medium text-charcoal text-sm truncate">${item.name}</p>
        <p class="text-sm font-bold" style="color:#b2571f;">${fmt(item.price)}</p>
        <p class="text-xs" style="color: rgba(44,44,44,0.4);">× ${item.quantity}</p>
      </div>
      <button class="remove-item-btn flex-shrink-0" style="color: rgba(44,44,44,0.3);" data-id="${item.id}" aria-label="Eliminar">
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
  cartItemsEl.querySelectorAll(".remove-item-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      cart = cart.filter((c) => c.id !== Number(btn.dataset.id));
      renderCart();
    });
  });
}
/* Agrega un event listener al botón "Finalizar Compra" en el carrito
 para llevar a la página de inicio de sesión (@{/login}, vía data-href) */
const cartCheckoutBtn = document.getElementById("cart-checkout");
if (cartCheckoutBtn) {
  cartCheckoutBtn.addEventListener("click", () => {
    window.location.href = cartCheckoutBtn.dataset.href;
  });
}

/*login (pestañas, accesos
de prueba y envío de los formularios). */
const loginForm = document.getElementById("login-form");
const signupForm = document.getElementById("signup-form");

/* Campos del formulario de inicio de sesión y registro */
const loginEmailInput = document.getElementById("login-email");
const loginPasswordInput = document.getElementById("login-password");
const signupNameInput = document.getElementById("signup-name");
const signupLastNameInput = document.getElementById("signup-lastname");
const signupEmailInput = document.getElementById("signup-email");
const signupPasswordInput = document.getElementById("signup-password");
const signupPhoneInput = document.getElementById("signup-phone");
const signupAddressInput = document.getElementById("signup-address");

/* Pestañas "Iniciar sesión" / "Registrarse" */
const authTabs = document.getElementById("auth-tabs");
const authSubtitle = document.getElementById("auth-subtitle");

/* Cambia a la pestaña indicada ("login" | "signup") */
function setAuthTab(tab) {
  if (!authTabs || !loginForm || !signupForm) return;
  const btn = authTabs.querySelector(`[data-tab="${tab}"]`);
  if (!btn) return;
  authTabs
    .querySelectorAll(".auth-tab")
    .forEach((b) => b.classList.remove("active"));
  btn.classList.add("active");
  const isLogin = tab === "login";
  loginForm.classList.toggle("hidden-modal", !isLogin);
  signupForm.classList.toggle("hidden-modal", isLogin);
  if (authSubtitle) {
    authSubtitle.textContent = isLogin
      ? "Bienvenido de vuelta"
      : "Únete a nosotros";
  }
}

if (authTabs && loginForm && signupForm) {
  authTabs.querySelectorAll(".auth-tab").forEach((btn) => {
    btn.addEventListener("click", () => setAuthTab(btn.dataset.tab));
  });

  /* Si se llega desde el nav-bar con "Registrarse" (@{/login(tab='register')}),
  abre directamente la pestaña de registro en lugar de la de inicio de sesión */
  const requestedTab = new URLSearchParams(window.location.search).get("tab");
  if (requestedTab === "register") {
    setAuthTab("signup");
  }
}

/* Usuarios de prueba */
const DEMO_USERS = {
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
página /admin pueda comprobar si quien la visita inició sesión como admin.*/
function saveSession(session) {
  localStorage.setItem("terraGalegaUser", JSON.stringify(session));
}

/* Accesos de prueba: solo rellenan el formulario, no autentican contra un backend real. */
document.querySelectorAll(".demo-login-btn").forEach((btn) => {
  btn.addEventListener("click", () => {
    if (loginEmailInput) loginEmailInput.value = btn.dataset.email;
    if (loginPasswordInput) loginPasswordInput.value = btn.dataset.password;
  });
});

/* Agrega event listeners a los formularios de inicio de sesión y registro 
para manejar la validación y el envío de datos */
if (loginForm) {
  loginForm.addEventListener("submit", (e) => {
    e.preventDefault();
    /* Obtiene los valores de correo electrónico y contraseña del formulario de inicio de sesión */
    const email = (loginEmailInput?.value || "").trim();
    const password = (loginPasswordInput?.value || "").trim();
    const errorEl = document.getElementById("login-error");
    /* Valida que ambos campos estén completos; si no, muestra un mensaje de error */
    if (!email || !password) {
      if (errorEl) {
        errorEl.textContent = "Por favor completa todos los campos.";
        errorEl.classList.remove("hidden-modal");
      }
      return;
    }

    /* Comprueba si el correo/contraseña corresponden a uno de los usuarios de
    prueba para saber si quien inicia sesión es un administrador */
    const demoUser = DEMO_USERS[email.toLowerCase()];
    if (demoUser && demoUser.password !== password) {
      if (errorEl) {
        errorEl.textContent = "Contraseña incorrecta.";
        errorEl.classList.remove("hidden-modal");
      }
      return;
    }

    /* Si la validación es exitosa, oculta el mensaje de error, guarda la sesión 
    y redirige según el rol: administradores al panel /admin, clientes al inicio */
    errorEl?.classList.add("hidden-modal");
    const session = demoUser
      ? { name: demoUser.name, email, role: demoUser.role }
      : { name: email.split("@")[0], email, role: "cliente" };
    saveSession(session);
    loginForm.reset();
    if (session.role === "admin") {
      window.location.href = "/admin";
    } else {
      alert("Bienvenido de nuevo a Terra Galega");
      window.location.href = "/";
    }
  });
}
/* Agrega un event listener al formulario de registro para manejar la validación y el envío de datos */
if (signupForm) {
  signupForm.addEventListener("submit", (e) => {
    e.preventDefault();
    /* Obtiene los valores de nombre, apellido, correo electrónico, contraseña,
     teléfono y dirección del formulario de registro */
    const name = (signupNameInput?.value || "").trim();
    const lastName = (signupLastNameInput?.value || "").trim();
    const email = (signupEmailInput?.value || "").trim();
    const password = (signupPasswordInput?.value || "").trim();
    const phone = (signupPhoneInput?.value || "").trim();
    const address = (signupAddressInput?.value || "").trim();
    const errorEl = document.getElementById("signup-error");
    /* Valida que todos los campos estén completos; si no, muestra un mensaje de error */
    if (!name || !lastName || !email || !password || !phone || !address) {
      if (errorEl) {
        errorEl.textContent = "Por favor completa todos los campos.";
        errorEl.classList.remove("hidden-modal");
      }
      return;
    }
    /* Si la validación es exitosa, oculta el mensaje de error, guarda la sesión
    (rol "cliente"), muestra un mensaje de bienvenida y regresa al inicio */
    errorEl?.classList.add("hidden-modal");
    saveSession({ name, email, role: "cliente" });
    alert(
      `Bienvenido a Terra Galega, ${name}. Tu cuenta fue creada exitosamente.`,
    );
    signupForm.reset();
    window.location.href = "/";
  });
}
/* Agrega un event listener al formulario de contacto (página /contacto) para 
manejar la validación y el envío de datos. */
const contactForm = document.getElementById("contact-form");
if (contactForm) {
  contactForm.addEventListener("submit", (e) => {
    e.preventDefault();
    const name = (document.getElementById("contact-name")?.value || "").trim();
    const email = (
      document.getElementById("contact-email")?.value || ""
    ).trim();
    const message = (
      document.getElementById("contact-message")?.value || ""
    ).trim();
    const errorEl = document.getElementById("contact-error");
    /* Valida que todos los campos estén completos; si no, muestra un mensaje de error */
    if (!name || !email || !message) {
      if (errorEl) {
        errorEl.textContent = "Por favor completa todos los campos.";
        errorEl.classList.remove("hidden-modal");
      }
      return;
    }
    errorEl?.classList.add("hidden-modal");
    alert(
      `Gracias ${name}, hemos recibido tu mensaje. Te contactaremos pronto.`,
    );
    contactForm.reset();
  });
}

/* Renderiza los testimonios de clientes en la sección correspondiente */
const TESTIMONIALS = [
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
function renderTestimonials() {
  const grid = document.getElementById("testimonials-grid");
  if (!grid) return;
  /* Genera el HTML para cada testimonio y lo inserta en el contenedor */
  const star = `<svg class="w-4 h-4" fill="#d99a21" viewBox="0 0 20 20"><path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/></svg>`;
  grid.innerHTML = TESTIMONIALS.map(
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

/* prdocutDetail/{id} (detalle de producto).*/
const detailAddBtn = document.getElementById("detail-add-btn");
if (detailAddBtn && typeof currentProduct !== "undefined" && currentProduct) {
  let detailQty = 1;
  let detailSelectedAdds = [];

  const detailQtyValueEl = document.getElementById("detail-qty-value");
  const detailQtyMinusBtn = document.getElementById("detail-qty-minus");
  const detailQtyPlusBtn = document.getElementById("detail-qty-plus");
  const detailTotalEl = document.getElementById("detail-total");
  const detailAddOptions = document.querySelectorAll(".detail-add-option");

  /* Calcula el total actual: precio base + adicionales seleccionados, por la cantidad */
  function detailComputeTotal() {
    const addsTotal = currentProduct.additionals
      .filter((a) => detailSelectedAdds.includes(a.name))
      .reduce((sum, a) => sum + a.price, 0);
    return (currentProduct.price + addsTotal) * detailQty;
  }

  /* Repinta cantidad, total y el botón de "Añadir a la orden" */
  function detailRender() {
    if (detailQtyValueEl) detailQtyValueEl.textContent = detailQty;
    const total = detailComputeTotal();
    if (detailTotalEl) detailTotalEl.textContent = fmt(total);
    detailAddBtn.textContent = `Añadir a la orden — ${fmt(total)}`;
  }

  /* Alterna el estilo (fondo/borde) de un adicional seleccionado, igual que en el mockup */
  function detailStyleOption(label, selected) {
    label.style.background = selected
      ? "rgba(178,87,31,0.07)"
      : "rgba(44,44,44,0.04)";
    label.style.border = selected
      ? "1px solid rgba(178,87,31,0.25)"
      : "1px solid rgba(44,44,44,0.07)";
  }

  detailQtyMinusBtn?.addEventListener("click", () => {
    detailQty = Math.max(1, detailQty - 1);
    detailRender();
  });
  detailQtyPlusBtn?.addEventListener("click", () => {
    detailQty += 1;
    detailRender();
  });

  detailAddOptions.forEach((label) => {
    const checkbox = label.querySelector("input[type=checkbox]");
    const name = label.dataset.name;
    checkbox?.addEventListener("change", () => {
      if (detailSelectedAdds.includes(name)) {
        detailSelectedAdds = detailSelectedAdds.filter((a) => a !== name);
      } else {
        detailSelectedAdds.push(name);
      }
      detailStyleOption(label, detailSelectedAdds.includes(name));
      detailRender();
    });
  });

  detailAddBtn.addEventListener("click", () => {
    const addsTotal = currentProduct.additionals
      .filter((a) => detailSelectedAdds.includes(a.name))
      .reduce((sum, a) => sum + a.price, 0);
    const unitPrice = currentProduct.price + addsTotal;
    /* Se agrega al mismo carrito global que usa el modal de /menu, con la
    misma forma de objeto que espera renderCart() */
    const existing = cart.find((c) => c.id === currentProduct.id);
    if (existing) {
      existing.quantity += detailQty;
    } else {
      cart.push({
        id: currentProduct.id,
        name: currentProduct.name,
        price: unitPrice,
        imageUrl: currentProduct.imageUrl,
        quantity: detailQty,
      });
    }
    renderCart();

    
    const originalText = detailAddBtn.textContent;
    const originalBackground = detailAddBtn.style.background;
    detailAddBtn.textContent = "¡Añadido a la orden!";
    detailAddBtn.style.background =
      "linear-gradient(135deg, #556141, #3d4730)";
    setTimeout(() => {
      detailAddBtn.style.background = originalBackground;
      detailRender();
    }, 2000);
  });

  detailRender();
}

/* Renderiza el carrito y los testimonios al cargar la página */
renderCart();
renderTestimonials();
