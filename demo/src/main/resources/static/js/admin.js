/* Lógica exclusiva de la página /admin: control de acceso, pestañas del
sidebar, buscador de productos y el modal de añadir/editar producto.
"adminProducts" viene inyectado desde admin.html (th:inline="javascript"),
igual que "menuProducts" en las páginas públicas. */

/* ─── Control de acceso ───
Como el proyecto no usa Spring Security, la sesión se guarda en el navegador
(localStorage) al iniciar sesión (ver script.js). Aquí solo comprobamos que
exista una sesión con rol "admin"; si no, se redirige a /login. */
(function guardAdminAccess() {
  let session = null;
  try {
    session = JSON.parse(localStorage.getItem("terraGalegaUser"));
  } catch (e) {
    session = null;
  }
  if (!session || session.role !== "admin") {
    window.location.href = "/login";
    return;
  }
  const nameEl = document.getElementById("admin-user-name");
  if (nameEl && session.name) nameEl.textContent = session.name;
})();

/* Cierra sesión: borra la sesión guardada y vuelve al inicio */
const adminLogoutBtn = document.getElementById("admin-logout");
if (adminLogoutBtn) {
  adminLogoutBtn.addEventListener("click", () => {
    localStorage.removeItem("terraGalegaUser");
    window.location.href = "/";
  });
}

/* ─── Pestañas del sidebar (Productos / Domicilios / Usuarios) ─── */
const ADMIN_TAB_TITLES = {
  productos: "Productos",
  pedidos: "Domicilios en curso",
  usuarios: "Usuarios",
};
const adminTabBtns = document.querySelectorAll(".admin-tab-btn");
const adminTitle = document.getElementById("admin-title");
const adminAddBtn = document.getElementById("admin-add-btn");

adminTabBtns.forEach((btn) => {
  btn.addEventListener("click", () => {
    const tab = btn.dataset.adminTab;
    adminTabBtns.forEach((b) => b.classList.remove("active"));
    btn.classList.add("active");
    document.querySelectorAll(".admin-panel").forEach((panel) => {
      panel.classList.add("hidden-modal");
    });
    document.getElementById(`admin-panel-${tab}`)?.classList.remove("hidden-modal");
    if (adminTitle) adminTitle.textContent = ADMIN_TAB_TITLES[tab] || tab;
    /* El botón "Añadir producto" solo tiene sentido en la pestaña de productos */
    if (adminAddBtn) adminAddBtn.classList.toggle("hidden-modal", tab !== "productos");
  });
});

/* ─── Buscador de productos (filtra las filas de la tabla) ─── */
const adminSearch = document.getElementById("admin-search");
const adminProductsEmpty = document.getElementById("admin-products-empty");
if (adminSearch) {
  adminSearch.addEventListener("input", () => {
    const term = adminSearch.value.trim().toLowerCase();
    const rows = document.querySelectorAll(".admin-product-row");
    let visible = 0;
    rows.forEach((row) => {
      const matches = (row.dataset.name || "").toLowerCase().includes(term);
      row.style.display = matches ? "" : "none";
      if (matches) visible++;
    });
    if (adminProductsEmpty) {
      adminProductsEmpty.classList.toggle("hidden-modal", visible !== 0);
    }
  });
}

/* ─── Modal de producto (añadir / editar) ─── */
const productModal = document.getElementById("admin-product-modal");
const productForm = document.getElementById("admin-product-form");
const modalTitle = document.getElementById("admin-modal-title");
const modalSubmitLabel = document.getElementById("admin-modal-submit-label");
const additionalsList = document.getElementById("admin-additionals-list");
const imageInput = document.getElementById("admin-f-image");
const imagePreview = document.getElementById("admin-f-image-preview");

/* Guarda temporalmente los adicionales del producto que se está creando/editando */
let currentAdditionals = [];

function renderAdditionals() {
  if (!additionalsList) return;
  additionalsList.innerHTML = "";
  currentAdditionals.forEach((add, i) => {
    const row = document.createElement("div");
    row.className = "admin-additional-row flex items-center justify-between px-3 py-2 rounded-lg";
    row.innerHTML = `
      <span class="text-sm" style="color: rgba(44,44,44,0.7)">${add.name} — +$${Number(add.price).toLocaleString("es-CO")}</span>
      <input type="hidden" name="additionalNames" value="${add.name}" />
      <input type="hidden" name="additionalPrices" value="${add.price}" />
      <button type="button" class="admin-remove-additional" data-index="${i}" style="color: rgba(44,44,44,0.3)">✕</button>
    `;
    additionalsList.appendChild(row);
  });
  additionalsList.querySelectorAll(".admin-remove-additional").forEach((btn) => {
    btn.addEventListener("click", () => {
      currentAdditionals.splice(Number(btn.dataset.index), 1);
      renderAdditionals();
    });
  });
}

const addAdditionalBtn = document.getElementById("admin-add-additional-btn");
const addNameInput = document.getElementById("admin-add-name");
const addPriceInput = document.getElementById("admin-add-price");
if (addAdditionalBtn) {
  addAdditionalBtn.addEventListener("click", () => {
    const name = addNameInput.value.trim();
    const price = addPriceInput.value.trim();
    if (!name || !price) return;
    currentAdditionals.push({ name, price: Number(price) });
    addNameInput.value = "";
    addPriceInput.value = "";
    renderAdditionals();
  });
}

/* Muestra la previsualización de la imagen mientras se escribe la URL */
if (imageInput) {
  imageInput.addEventListener("input", () => {
    const url = imageInput.value.trim();
    if (url) {
      imagePreview.src = url;
      imagePreview.classList.remove("hidden-modal");
    } else {
      imagePreview.classList.add("hidden-modal");
    }
  });
}

function openModal() {
  productModal.classList.remove("hidden-modal");
}
function closeModal() {
  productModal.classList.add("hidden-modal");
}

/* Abre el modal en modo "Añadir producto" (formulario vacío) */
function openAddModal() {
  productForm.reset();
  productForm.setAttribute("action", "/admin/products");
  modalTitle.textContent = "Añadir producto";
  modalSubmitLabel.textContent = "Añadir producto";
  currentAdditionals = [];
  renderAdditionals();
  imagePreview.classList.add("hidden-modal");
  openModal();
}

/* Abre el modal en modo "Editar producto", precargando los datos del producto */
function openEditModal(id) {
  const product = adminProducts.find((p) => p.id === id);
  if (!product) return;
  productForm.reset();
  productForm.setAttribute("action", `/admin/products/${id}/update`);
  modalTitle.textContent = "Editar producto";
  modalSubmitLabel.textContent = "Guardar cambios";

  document.getElementById("admin-f-name").value = product.name || "";
  document.getElementById("admin-f-price").value = product.price || "";
  document.getElementById("admin-f-category").value = product.category || "";
  document.getElementById("admin-f-description").value = product.description || "";
  document.getElementById("admin-f-image").value = product.imageUrl || "";
  document.getElementById("admin-f-popular").checked = !!product.popular;

  if (product.imageUrl) {
    imagePreview.src = product.imageUrl;
    imagePreview.classList.remove("hidden-modal");
  } else {
    imagePreview.classList.add("hidden-modal");
  }

  currentAdditionals = (product.additionals || []).map((a) => ({ name: a.name, price: a.price }));
  renderAdditionals();
  openModal();
}

if (adminAddBtn) adminAddBtn.addEventListener("click", openAddModal);

document.querySelectorAll(".admin-edit-btn").forEach((btn) => {
  btn.addEventListener("click", () => openEditModal(Number(btn.dataset.id)));
});

const modalCloseBtn = document.getElementById("admin-modal-close");
const modalCancelBtn = document.getElementById("admin-modal-cancel");
if (modalCloseBtn) modalCloseBtn.addEventListener("click", closeModal);
if (modalCancelBtn) modalCancelBtn.addEventListener("click", closeModal);
if (productModal) {
  productModal.addEventListener("click", (e) => {
    if (e.target === productModal) closeModal();
  });
}
