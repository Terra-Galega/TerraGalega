/* Formatea un número como precio en pesos colombianos */
const fmt = (price) =>
  new Intl.NumberFormat("es-CO", {
    style: "currency",
    currency: "COP",
    maximumFractionDigits: 0,
  }).format(price);


/*Variables para gestionar el carrito de compras */
let cart = [];
let currentCategory = "Todos";
let selectedItem = null;
let qty = 1;
let selectedAdds = [];
/* Elementos del DOM para gestionar el carrito */
const menuGrid = document.getElementById("menu-grid");
const categoryTabs = document.getElementById("category-tabs");
const cartCountDesktop = document.getElementById("cart-count-desktop");
const cartCountMobile = document.getElementById("cart-count-mobile");
const navToggle = document.getElementById("nav-toggle");
const mobileMenu = document.getElementById("mobile-menu");
const iconOpen = document.getElementById("icon-open");
const iconClose = document.getElementById("icon-close");

/* Función para abrir el modal de un producto específico mediante su ID enviado desde Thymeleaf */
function openProductById(id) {
  const item = menuProducts.find(p => p.id === id);
  if (item) {
    openProduct(item);
  }
}

/* Maneja el cambio de categoría al hacer clic en los botones de categoría */
/* Se protege con un if porque no todas las páginas tienen la grilla completa del menú (p. ej. el home solo muestra "Favoritos") */
if (categoryTabs) {
  categoryTabs.querySelectorAll(".cat-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      /* Actualiza la categoría actual eliminando la clase "active" de 
      todos los botones y agregando la clase a aquel que fue CLICK */
      currentCategory = btn.dataset.category;
      categoryTabs
        .querySelectorAll(".cat-btn")
        .forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");

      /* Filtra los elementos del DOM renderizados por Thymeleaf según la nueva categoría */
      document.querySelectorAll('.menu-item').forEach(item => {
        if (currentCategory === 'Todos' || item.dataset.category === currentCategory) {
          item.style.display = 'block';
        } else {
          item.style.display = 'none';
        }
      });
    });
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

/* Maneja la apertura y cierre del menú móvil al hacer clic en el botón de navegación */
navToggle.addEventListener("click", () => {
  const isOpen = !mobileMenu.classList.contains("hidden-modal");
  /* Si el menú móvil está abierto, lo cierra; de lo contrario, lo abre y ajusta los íconos */
  if (isOpen) {
    closeMobileMenu();
  } else {
    /* Abre el menú móvil y ajusta los íconos */
    mobileMenu.classList.remove("hidden-modal");
    iconOpen.classList.add("hidden-modal");
    iconClose.classList.remove("hidden-modal");
  }
});
/* Función para cerrar el menú móvil y ajustar los íconos */
function closeMobileMenu() {
  mobileMenu.classList.add("hidden-modal");
  iconOpen.classList.remove("hidden-modal");
  iconClose.classList.add("hidden-modal");
}
/* Maneja la apertura y cierre de los modales (login, signup, cart, product) */
/*modals=div que se esconden a menos que se abran con un click */
const modals = {
  login: document.getElementById("modal-login"),
  signup: document.getElementById("modal-signup"),
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
  Object.values(modals).forEach((m) => m.classList.add("hidden-modal"));
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
hacer clic fuera del contenido */
Object.values(modals).forEach((modal) => {
  modal.addEventListener("click", (e) => {
    if (e.target === modal) closeAllModals();
  });
});

/* Agrega un event listener al botón "Explorar Menú" en el 
carrito para cerrar todos los modales y hacer scroll hacia la sección del menú */
document.getElementById("cart-explore-menu").addEventListener("click", () => {
  closeAllModals();
  document.getElementById("menu")?.scrollIntoView({ behavior: "smooth" });
});

/* Función para abrir el modal de un producto específico */
function openProduct(item) {
  selectedItem = item;
  qty = 1;
  selectedAdds = [];
  renderProductModal();
  openModal("product");
}
/* Función para renderizar el contenido del modal de producto */
function renderProductModal() {
  if (!selectedItem) return;
  /* Actualiza el contenido del modal con la información del producto seleccionado */
  /* Adaptado para consumir 'imageUrl' que proviene de Spring Boot */
  document.getElementById("product-image").src = selectedItem.imageUrl;
  document.getElementById("product-image").alt = selectedItem.name;
  document.getElementById("product-name").textContent = selectedItem.name;
  document.getElementById("product-price").textContent = fmt(
    selectedItem.price,
  );
  /* Actualiza la descripción del producto y la cantidad seleccionada */
  document.getElementById("product-description").textContent =
    selectedItem.description;
  document.getElementById("qty-value").textContent = qty;
/* Actualiza la sección de adicionales del producto */
  const addsWrap = document.getElementById("product-additionals-wrap");
  const addsContainer = document.getElementById("product-additionals");
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
        <input type="checkbox" data-add="${i}" ${selectedAdds.includes(add.name) ? "checked" : ""} style="accent-color:#b2571f; width:16px; height:16px; flex-shrink:0;" />
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
        const add = selectedItem.additionals[i];
        if (selectedAdds.includes(add.name)) {
          selectedAdds = selectedAdds.filter((a) => a !== add.name);
        } else {
          selectedAdds.push(add.name);
        }
      });
    });
  }
/* Actualiza el texto del botón "Agregar al carrito" 
con el precio total según la cantidad seleccionada */
  document.getElementById("add-to-cart-btn").textContent =
    `Agregar al carrito — ${fmt(selectedItem.price * qty)}`;
}

/* Agrega event listeners a los botones de cantidad para aumentar o
 disminuir la cantidad seleccionada */
 /* Agrega event listener al botón de disminuir cantidad */
document.getElementById("qty-minus").addEventListener("click", () => {
  qty = Math.max(1, qty - 1);
  renderProductModal();
});
/* Agrega event listener al botón de aumentar cantidad */
document.getElementById("qty-plus").addEventListener("click", () => {
  qty = qty + 1;
  renderProductModal();
});
/* Agrega event listener al botón "Agregar al carrito" para agregar 
el producto seleccionado al carrito */
document.getElementById("add-to-cart-btn").addEventListener("click", () => {
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

/* Función para renderizar el contenido del carrito de compras 
ACTUALIZAR CARRITO*/
function renderCart() {
  const count = cart.reduce((s, i) => s + i.quantity, 0);
  const total = cart.reduce((s, i) => s + i.price * i.quantity, 0);
  /* Actualiza el contador de elementos en el carrito en la interfaz de usuario */
  [cartCountDesktop, cartCountMobile].forEach((el) => {
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
  document.getElementById("cart-total").textContent = fmt(total);
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
      <button class="remove-item flex-shrink-0" style="color: rgba(44,44,44,0.3);" data-id="${item.id}" aria-label="Eliminar">
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
  cartItemsEl.querySelectorAll(".remove-item").forEach((btn) => {
    btn.addEventListener("click", () => {
      cart = cart.filter((c) => c.id !== Number(btn.dataset.id));
      renderCart();
    });
  });
}
/* Agrega un event listener al botón "Finalizar Compra" en el carrito
 para abrir el modal de inicio de sesión */
document.getElementById("cart-checkout").addEventListener("click", () => {
  openModal("login");
});
/* Agrega event listeners a los formularios de inicio de sesión y registro 
para manejar la validación y el envío de datos */
document.getElementById("login-form").addEventListener("submit", (e) => {
  e.preventDefault();
  /* Obtiene los valores de correo electrónico y contraseña del formulario de inicio de sesión */
  const email = document.getElementById("login-email").value.trim();
  const password = document.getElementById("login-password").value.trim();
  const errorEl = document.getElementById("login-error");
  /* Valida que ambos campos estén completos; si no, muestra un mensaje de error */
  if (!email || !password) {
    errorEl.textContent = "Por favor completa todos los campos.";
    errorEl.classList.remove("hidden-modal");
    return;
  }
  /* Si la validación es exitosa, oculta el mensaje de error, muestra un mensaje de bienvenida, 
  cierra todos los modales y reinicia el formulario de inicio de sesión */
  errorEl.classList.add("hidden-modal");
  alert("Bienvenido de nuevo a Terra Galega");
  closeAllModals();
  document.getElementById("login-form").reset();
});
/* Agrega un event listener al formulario de registro para manejar la validación y el envío de datos */
document.getElementById("signup-form").addEventListener("submit", (e) => {
  e.preventDefault();
  /* Obtiene los valores de nombre, apellido, correo electrónico, contraseña,
   teléfono y dirección del formulario de registro */
  const name = document.getElementById("signup-name").value.trim();
  const lastName = document.getElementById("signup-lastname").value.trim();
  const email = document.getElementById("signup-email").value.trim();
  const password = document.getElementById("signup-password").value.trim();
  const phone = document.getElementById("signup-phone").value.trim();
  const address = document.getElementById("signup-address").value.trim();
  const errorEl = document.getElementById("signup-error");
  /* Valida que todos los campos estén completos; si no, muestra un mensaje de error */
  if (!name || !lastName || !email || !password || !phone || !address) {
    errorEl.textContent = "Por favor completa todos los campos.";
    errorEl.classList.remove("hidden-modal");
    return;
  }
  /* Si la validación es exitosa, oculta el mensaje de error, muestra un mensaje de bienvenida,
  cierra todos los modales y reinicia el formulario de registro */
  errorEl.classList.add("hidden-modal");
  alert(
    `Bienvenido a Terra Galega, ${name}. Tu cuenta fue creada exitosamente.`,
  );
  closeAllModals();
  document.getElementById("signup-form").reset();
});
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

/* Renderiza el carrito y los testimonios al cargar la página */
renderCart();
renderTestimonials();