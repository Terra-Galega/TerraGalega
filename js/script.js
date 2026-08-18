const MENU = [
  {
    id: 1,
    name: "Pulpo a la Gallega",
    description:
      "Tierno pulpo cocido sobre cama de patatas con pimentón de la Vera y aceite de oliva virgen extra.",
    price: 38900,
    category: "Entradas",
    image:
      "https://images.unsplash.com/photo-1534080564583-6be75777b70a?w=480&h=360&fit=crop&auto=format",
    additionals: ["Salsa picante +$3.000", "Pan de maíz +$4.000"],
  },
  {
    id: 2,
    name: "Croquetas de Jamón Ibérico",
    description:
      "Cremosas croquetas de jamón ibérico con bechamel artesanal, doradas en aceite de oliva de arbequina.",
    price: 24500,
    category: "Entradas",
    image:
      "https://images.unsplash.com/photo-1588276552401-30058a0fe57b?w=480&h=360&fit=crop&auto=format",
    additionals: ["Salsa brava +$2.500", "Alioli casero +$2.000"],
  },
  {
    id: 3,
    name: "Empanada Gallega",
    description:
      "Empanada tradicional con atún del norte, pimientos asados y cebolla caramelizada al estilo gallego.",
    price: 22000,
    category: "Entradas",
    image:
      "https://images.unsplash.com/photo-1650964807311-970cb88d347c?w=480&h=360&fit=crop&auto=format",
    additionals: ["Ensalada verde +$5.000"],
  },
  {
    id: 4,
    name: "Gambas a la Plancha",
    description:
      "Langostinos frescos a la plancha con mantequilla de ajo, perejil fresco y limón de Murcia.",
    price: 48500,
    category: "Mariscos",
    image:
      "https://images.unsplash.com/photo-1515443961218-a51367888e4b?w=480&h=360&fit=crop&auto=format",
    additionals: ["Extra ajo +$2.000", "Salsa de limón +$3.000"],
  },
  {
    id: 5,
    name: "Mariscos al Ajillo",
    description:
      "Selección de mariscos frescos salteados en aceite de ajo con vino Albariño y guindilla roja.",
    price: 62000,
    category: "Mariscos",
    image:
      "https://images.unsplash.com/photo-1621841957884-1210fe19d66d?w=480&h=360&fit=crop&auto=format",
    additionals: ["Pan rústico +$4.000", "Ensalada del mar +$8.000"],
  },
  {
    id: 6,
    name: "Paella de Mariscos",
    description:
      "Arroz bomba al azafrán con gambas reales, mejillones, almejas y calamar fresco de la costa gallega.",
    price: 75000,
    category: "Mariscos",
    image:
      "https://images.unsplash.com/photo-1783685739826-335e8133a197?w=480&h=360&fit=crop&auto=format",
    additionals: ["Alioli casero +$3.000", "Limón extra +$1.000"],
  },
  {
    id: 7,
    name: "Lacón con Grelos",
    description:
      "Codillo gallego curado, chorizo ahumado y grelos tiernos cocidos lentamente en caldo de verduras.",
    price: 55000,
    category: "Carnes",
    image:
      "https://images.unsplash.com/photo-1623961990059-28356e226a77?w=480&h=360&fit=crop&auto=format",
    additionals: ["Pan de millo +$3.500", "Cachelos +$6.000"],
  },
  {
    id: 8,
    name: "Solomillo a la Brasa",
    description:
      "Solomillo Angus a la brasa con chimichurri gallego y reducción de vino Ribeiro sobre pizarra.",
    price: 82000,
    category: "Carnes",
    image:
      "https://images.unsplash.com/photo-1554371650-4484f3a102f2?w=480&h=360&fit=crop&auto=format",
    additionals: ["Patatas bravas +$7.000", "Pimientos asados +$5.000"],
  },
  {
    id: 9,
    name: "Tarta de Santiago",
    description:
      "Clásica tarta de almendra gallega, aromatizada con limón y canela, decorada con la Cruz de Santiago.",
    price: 18000,
    category: "Postres",
    image:
      "https://images.unsplash.com/photo-1534080564583-6be75777b70a?w=480&h=360&fit=crop&auto=format",
    additionals: ["Nata montada +$3.000", "Helado de vainilla +$4.000"],
  },
  {
    id: 10,
    name: "Filloas con Crema",
    description:
      "Filloas gallegas delicadas rellenas de crema de vainilla con coulis de frutos rojos del bosque.",
    price: 16500,
    category: "Postres",
    image:
      "https://images.unsplash.com/photo-1588276552401-30058a0fe57b?w=480&h=360&fit=crop&auto=format",
    additionals: ["Chocolate caliente +$3.500", "Frutos rojos extra +$4.000"],
  },
];

const fmt = (price) =>
  new Intl.NumberFormat("es-CO", {
    style: "currency",
    currency: "COP",
    maximumFractionDigits: 0,
  }).format(price);

let cart = [];
let currentCategory = "Todos";
let selectedItem = null;
let qty = 1;
let selectedAdds = [];

const menuGrid = document.getElementById("menu-grid");
const categoryTabs = document.getElementById("category-tabs");
const cartCountDesktop = document.getElementById("cart-count-desktop");
const cartCountMobile = document.getElementById("cart-count-mobile");
const navToggle = document.getElementById("nav-toggle");
const mobileMenu = document.getElementById("mobile-menu");
const iconOpen = document.getElementById("icon-open");
const iconClose = document.getElementById("icon-close");

function renderMenu() {
  const items =
    currentCategory === "Todos"
      ? MENU
      : MENU.filter((i) => i.category === currentCategory);

  menuGrid.innerHTML = items
    .map(
      (item) => `
    <article class="bg-white overflow-hidden shadow-sm group" style="border-radius:2px;">
      <div class="relative h-52 overflow-hidden" style="background-color: rgba(44,44,44,0.08);">
        <img src="${item.image}" alt="${item.name}" class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105" />
        <span class="absolute top-3 right-3 text-xs font-medium px-2.5 py-1 rounded-full" style="background-color: rgba(44,44,44,0.78); color:#f5f2eb;">${item.category}</span>
      </div>
      <div class="p-5">
        <h3 class="font-serif text-xl font-semibold text-charcoal mb-2">${item.name}</h3>
        <p class="text-sm leading-relaxed mb-4 line-clamp-2" style="color: rgba(44,44,44,0.6);">${item.description}</p>
        ${
          item.additionals.length > 0
            ? `<p class="text-xs mb-4" style="color: rgba(44,44,44,0.4);">${item.additionals.length} adicional${item.additionals.length > 1 ? "es" : ""} disponible${item.additionals.length > 1 ? "s" : ""}</p>`
            : ""
        }
        <div class="flex items-center justify-between">
          <span class="font-bold text-lg" style="color:#b2571f;">${fmt(item.price)}</span>
          <button class="open-product px-4 py-2 text-white text-sm font-medium rounded btn-terra" data-id="${item.id}">Agregar</button>
        </div>
      </div>
    </article>
  `,
    )
    .join("");

  menuGrid.querySelectorAll(".open-product").forEach((btn) => {
    btn.addEventListener("click", () => {
      const item = MENU.find((m) => m.id === Number(btn.dataset.id));
      openProduct(item);
    });
  });
}

categoryTabs.querySelectorAll(".cat-btn").forEach((btn) => {
  btn.addEventListener("click", () => {
    currentCategory = btn.dataset.category;
    categoryTabs
      .querySelectorAll(".cat-btn")
      .forEach((b) => b.classList.remove("active"));
    btn.classList.add("active");
    renderMenu();
  });
});

document.querySelectorAll("[data-scroll]").forEach((el) => {
  el.addEventListener("click", () => {
    const id = el.dataset.scroll;
    document.getElementById(id)?.scrollIntoView({ behavior: "smooth" });
    closeMobileMenu();
  });
});

navToggle.addEventListener("click", () => {
  const isOpen = !mobileMenu.classList.contains("hidden-modal");
  if (isOpen) {
    closeMobileMenu();
  } else {
    mobileMenu.classList.remove("hidden-modal");
    iconOpen.classList.add("hidden-modal");
    iconClose.classList.remove("hidden-modal");
  }
});

function closeMobileMenu() {
  mobileMenu.classList.add("hidden-modal");
  iconOpen.classList.remove("hidden-modal");
  iconClose.classList.add("hidden-modal");
}

const modals = {
  login: document.getElementById("modal-login"),
  signup: document.getElementById("modal-signup"),
  cart: document.getElementById("modal-cart"),
  product: document.getElementById("modal-product"),
};

function openModal(name) {
  closeAllModals();
  modals[name]?.classList.remove("hidden-modal");
}

function closeAllModals() {
  Object.values(modals).forEach((m) => m.classList.add("hidden-modal"));
}

document.querySelectorAll("[data-modal]").forEach((el) => {
  el.addEventListener("click", () => {
    openModal(el.dataset.modal);
    closeMobileMenu();
  });
});

document.querySelectorAll("[data-close]").forEach((el) => {
  el.addEventListener("click", () => closeAllModals());
});

Object.values(modals).forEach((modal) => {
  modal.addEventListener("click", (e) => {
    if (e.target === modal) closeAllModals();
  });
});

document.getElementById("cart-explore-menu").addEventListener("click", () => {
  closeAllModals();
  document.getElementById("menu")?.scrollIntoView({ behavior: "smooth" });
});

function openProduct(item) {
  selectedItem = item;
  qty = 1;
  selectedAdds = [];
  renderProductModal();
  openModal("product");
}

function renderProductModal() {
  if (!selectedItem) return;
  document.getElementById("product-image").src = selectedItem.image;
  document.getElementById("product-image").alt = selectedItem.name;
  document.getElementById("product-name").textContent = selectedItem.name;
  document.getElementById("product-price").textContent = fmt(
    selectedItem.price,
  );
  document.getElementById("product-description").textContent =
    selectedItem.description;
  document.getElementById("qty-value").textContent = qty;

  const addsWrap = document.getElementById("product-additionals-wrap");
  const addsContainer = document.getElementById("product-additionals");
  if (selectedItem.additionals.length === 0) {
    addsWrap.classList.add("hidden-modal");
  } else {
    addsWrap.classList.remove("hidden-modal");
    addsContainer.innerHTML = selectedItem.additionals
      .map(
        (add, i) => `
      <label class="flex items-center gap-3 cursor-pointer select-none">
        <input type="checkbox" data-add="${i}" ${selectedAdds.includes(add) ? "checked" : ""} style="accent-color:#b2571f; width:16px; height:16px; flex-shrink:0;" />
        <span class="text-sm" style="color: rgba(44,44,44,0.7);">${add}</span>
      </label>
    `,
      )
      .join("");

    addsContainer.querySelectorAll("input[type=checkbox]").forEach((cb, i) => {
      cb.addEventListener("change", () => {
        const add = selectedItem.additionals[i];
        if (selectedAdds.includes(add)) {
          selectedAdds = selectedAdds.filter((a) => a !== add);
        } else {
          selectedAdds.push(add);
        }
      });
    });
  }

  document.getElementById("add-to-cart-btn").textContent =
    `Agregar al carrito — ${fmt(selectedItem.price * qty)}`;
}

document.getElementById("qty-minus").addEventListener("click", () => {
  qty = Math.max(1, qty - 1);
  renderProductModal();
});
document.getElementById("qty-plus").addEventListener("click", () => {
  qty = qty + 1;
  renderProductModal();
});

document.getElementById("add-to-cart-btn").addEventListener("click", () => {
  if (!selectedItem) return;
  const existing = cart.find((c) => c.id === selectedItem.id);
  if (existing) {
    existing.quantity += qty;
  } else {
    cart.push({ ...selectedItem, quantity: qty });
  }
  renderCart();
  closeAllModals();
});

function renderCart() {
  const count = cart.reduce((s, i) => s + i.quantity, 0);
  const total = cart.reduce((s, i) => s + i.price * i.quantity, 0);

  [cartCountDesktop, cartCountMobile].forEach((el) => {
    if (count > 0) {
      el.textContent = count;
      el.classList.remove("hidden-modal");
    } else {
      el.classList.add("hidden-modal");
    }
  });

  const cartItemsEl = document.getElementById("cart-items");
  const cartEmptyEl = document.getElementById("cart-empty");
  const cartFooterEl = document.getElementById("cart-footer");

  if (cart.length === 0) {
    cartEmptyEl.classList.remove("hidden-modal");
    cartItemsEl.innerHTML = "";
    cartFooterEl.classList.add("hidden-modal");
    return;
  }

  cartEmptyEl.classList.add("hidden-modal");
  cartFooterEl.classList.remove("hidden-modal");
  document.getElementById("cart-total").textContent = fmt(total);

  cartItemsEl.innerHTML = cart
    .map(
      (item) => `
    <div class="flex items-center gap-4 bg-white p-3" style="border-radius:2px;">
      <img src="${item.image}" alt="${item.name}" class="w-16 h-16 object-cover flex-shrink-0" style="border-radius:2px;" />
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

  cartItemsEl.querySelectorAll(".remove-item").forEach((btn) => {
    btn.addEventListener("click", () => {
      cart = cart.filter((c) => c.id !== Number(btn.dataset.id));
      renderCart();
    });
  });
}

document.getElementById("cart-checkout").addEventListener("click", () => {
  openModal("login");
});

document.getElementById("login-form").addEventListener("submit", (e) => {
  e.preventDefault();
  const email = document.getElementById("login-email").value.trim();
  const password = document.getElementById("login-password").value.trim();
  const errorEl = document.getElementById("login-error");

  if (!email || !password) {
    errorEl.textContent = "Por favor completa todos los campos.";
    errorEl.classList.remove("hidden-modal");
    return;
  }
  errorEl.classList.add("hidden-modal");
  alert("Bienvenido de nuevo a Terra Galega");
  closeAllModals();
  document.getElementById("login-form").reset();
});

document.getElementById("signup-form").addEventListener("submit", (e) => {
  e.preventDefault();
  const name = document.getElementById("signup-name").value.trim();
  const lastName = document.getElementById("signup-lastname").value.trim();
  const email = document.getElementById("signup-email").value.trim();
  const password = document.getElementById("signup-password").value.trim();
  const phone = document.getElementById("signup-phone").value.trim();
  const address = document.getElementById("signup-address").value.trim();
  const errorEl = document.getElementById("signup-error");

  if (!name || !lastName || !email || !password || !phone || !address) {
    errorEl.textContent = "Por favor completa todos los campos.";
    errorEl.classList.remove("hidden-modal");
    return;
  }
  errorEl.classList.add("hidden-modal");
  alert(
    `Bienvenido a Terra Galega, ${name}. Tu cuenta fue creada exitosamente.`,
  );
  closeAllModals();
  document.getElementById("signup-form").reset();
});

const TESTIMONIALS = [
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
function renderTestimonials() {
  const grid = document.getElementById("testimonials-grid");
  if (!grid) return;
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

renderMenu();
renderCart();
renderTestimonials();
