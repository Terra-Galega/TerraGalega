package com.example.demo.movil.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.demo.dto.CategoryRefDTO;
import com.example.demo.dto.CheckoutItemDTO;
import com.example.demo.dto.ProductSummaryDTO;
import com.example.demo.entities.AddOn;
import com.example.demo.entities.Product;
import com.example.demo.errors.ApiException;
import com.example.demo.movil.dto.CartDTOs.CartAddOnDTO;
import com.example.demo.movil.dto.CartDTOs.CartItemDTO;
import com.example.demo.movil.entity.CartLine;
import com.example.demo.movil.repository.CartLineRepository;
import com.example.demo.repository.AddOnRepository;
import com.example.demo.repository.ProductRepository;

/**
 * Carrito compartido entre la web y la app. Se guarda en PostgreSQL (ids de
 * producto,
 * cantidades y adicionales); los datos del producto se leen de H2 cada vez, así
 * que
 * precio, nombre e imagen siempre están al día.
 *
 * Los métodos públicos usan la transacción de H2 (solo lectura) para leer
 * productos y
 * adicionales; las escrituras en Postgres van con su propia transacción.
 */
@Service
public class CartService {

    private static final int MAX_QUANTITY = 99;

    @Autowired
    private CartLineRepository repository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private AddOnRepository addOnRepository;
    @Autowired
    private com.example.demo.service.ClientService clientService;

    private final TransactionTemplate pgTx;
    @jakarta.persistence.PersistenceContext(unitName = "h2")
    private jakarta.persistence.EntityManager primaryEm;

    // Un candado por cliente: si la web y la app tocan el mismo carrito a la vez,
    // se atienden en fila
    private final ConcurrentHashMap<Integer, Object> locks = new ConcurrentHashMap<>();

    public CartService(@Qualifier("postgresTransactionManager") PlatformTransactionManager pgManager) {
        this.pgTx = new TransactionTemplate(pgManager);
    }

    @Transactional(readOnly = true)
    public List<CartItemDTO> getCart(Integer clientId) {
        clientService.getClientById(clientId); // 404 si el cliente no existe
        return buildCart(clientId);
    }

    // Suma una línea (si ya existe la misma con los mismos adicionales, suma la
    // cantidad)
    @Transactional
    public List<CartItemDTO> addItem(Integer clientId, CheckoutItemDTO item) {
        lockClient(clientId);
        clientService.getClientById(clientId);
        addLine(clientId, item);
        return buildCart(clientId);
    }

    // Une varias líneas de golpe (carrito de invitado de la web al iniciar sesión)
    @Transactional
    public List<CartItemDTO> addItems(Integer clientId, List<CheckoutItemDTO> items) {
        lockClient(clientId);
        clientService.getClientById(clientId);
        if (items != null) {
            for (CheckoutItemDTO item : items) {
                try {
                    addLine(clientId, item);
                } catch (ApiException e) {
                    // Un producto que ya no existe o no está disponible simplemente se omite
                }
            }
        }
        return buildCart(clientId);
    }

    @Transactional
    public List<CartItemDTO> setQuantity(Integer clientId, String lineId, int quantity) {
        lockClient(clientId);
        clientService.getClientById(clientId);
        if (quantity > MAX_QUANTITY) {
            throw ApiException.badRequest("Máximo " + MAX_QUANTITY + " unidades por producto.");
        }
        synchronized (lock(clientId)) {
            pgTx.executeWithoutResult(status -> {
                CartLine line = repository.findByClientIdAndLineId(clientId, lineId)
                        .orElseThrow(() -> ApiException.notFound("Ese producto ya no está en tu carrito."));
                if (quantity < 1) {
                    repository.delete(line);
                } else {
                    line.setQuantity(quantity);
                    line.setUpdatedAt(LocalDateTime.now());
                    repository.save(line);
                }
            });
        }
        return buildCart(clientId);
    }

    // Si la línea ya no existe no pasa nada (la otra pantalla pudo quitarla antes)
    @Transactional
    public List<CartItemDTO> removeItem(Integer clientId, String lineId) {
        lockClient(clientId);
        clientService.getClientById(clientId);
        deleteLine(clientId, lineId);
        return buildCart(clientId);
    }

    @Transactional
    public void clear(Integer clientId) {
        lockClient(clientId);
        synchronized (lock(clientId)) {
            pgTx.executeWithoutResult(status -> repository.deleteAll(repository.findByClientIdOrderByIdAsc(clientId)));
        }
    }

    // ---------------------------------------------------------------- internos

    private void addLine(Integer clientId, CheckoutItemDTO item) {
        Integer quantity = item.getQuantity();
        if (item.getProductId() == null) {
            throw ApiException.badRequest("Falta el producto.");
        }
        if (quantity == null || quantity < 1) {
            throw ApiException.badRequest("Cantidad inválida para el producto " + item.getProductId());
        }
        if (quantity > MAX_QUANTITY) {
            throw ApiException.badRequest("Máximo " + MAX_QUANTITY + " unidades por producto.");
        }

        Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> ApiException.conflict("El producto ya no está disponible."));
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw ApiException.conflict("El producto \"" + product.getName() + "\" ya no está disponible.");
        }

        List<Integer> addOnIds = item.getAddOnIds() == null ? List.of() : item.getAddOnIds();
        for (Integer addOnId : addOnIds) {
            AddOn addOn = addOnId == null ? null : addOnRepository.findById(addOnId).orElse(null);
            if (addOn == null) {
                throw ApiException.conflict("Adicional no encontrado: " + addOnId);
            }
            boolean allowed = product.getCategory() != null && addOn.getCategories().stream()
                    .anyMatch(c -> c.getId().equals(product.getCategory().getId()));
            if (!allowed) {
                throw ApiException.badRequest("El adicional seleccionado no está disponible para este producto.");
            }
        }

        String lineId = lineId(product.getId(), addOnIds);
        String storedAddOns = addOnIds.stream().sorted().map(String::valueOf).collect(Collectors.joining(","));

        synchronized (lock(clientId)) {
            pgTx.executeWithoutResult(status -> {
                LocalDateTime now = LocalDateTime.now();
                CartLine line = repository.findByClientIdAndLineId(clientId, lineId).orElse(null);
                if (line == null) {
                    line = new CartLine();
                    line.setClientId(clientId);
                    line.setLineId(lineId);
                    line.setProductId(product.getId());
                    line.setQuantity(quantity);
                    line.setAddOnIds(storedAddOns);
                    line.setCreatedAt(now);
                } else {
                    line.setQuantity(Math.min(line.getQuantity() + quantity, MAX_QUANTITY));
                }
                line.setUpdatedAt(now);
                repository.save(line);
            });
        }
    }

    private void deleteLine(Integer clientId, String lineId) {
        synchronized (lock(clientId)) {
            pgTx.executeWithoutResult(status -> repository.findByClientIdAndLineId(clientId, lineId)
                    .ifPresent(repository::delete));
        }
    }

    private List<CartItemDTO> buildCart(Integer clientId) {
        List<CartItemDTO> result = new ArrayList<>();
        for (CartLine line : repository.findByClientIdOrderByIdAsc(clientId)) {
            Product product = productRepository.findById(line.getProductId()).orElse(null);
            if (product == null) {
                deleteLine(clientId, line.getLineId()); // el producto se eliminó del menú
                continue;
            }
            List<CartAddOnDTO> addOns = new ArrayList<>();
            for (Integer addOnId : parseIds(line.getAddOnIds())) {
                addOnRepository.findById(addOnId)
                        .ifPresent(a -> addOns.add(new CartAddOnDTO(a.getId(), a.getName(), a.getPrice())));
            }
            // Un producto desactivado se devuelve igual (active = false): cada pantalla
            // avisa y lo quita
            result.add(new CartItemDTO(line.getLineId(), toSummary(product), line.getQuantity(), addOns));
        }
        return result;
    }

    // Misma regla que buildId() de la web
    private String lineId(Integer productId, List<Integer> addOnIds) {
        return productId + ":" + addOnIds.stream().sorted().map(String::valueOf).collect(Collectors.joining("-"));
    }

    private List<Integer> parseIds(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(Integer::valueOf)
                .toList();
    }

    private ProductSummaryDTO toSummary(Product p) {
        CategoryRefDTO category = null;
        if (p.getCategory() != null) {
            category = new CategoryRefDTO();
            category.setId(p.getCategory().getId());
            category.setName(p.getCategory().getName());
        }
        return new ProductSummaryDTO(p.getId(), p.getName(), p.getDescription(), p.getPrice(), category,
                p.getImageUrl(), p.getActive(), p.getPopular(), p.isVegetarian(), p.isSpicyMild(),
                p.isSpicyHot(), p.isContainsNuts(), p.isContainsSeafood(), p.isContainsGluten());
    }

    private Object lock(Integer clientId) {
        return locks.computeIfAbsent(clientId, k -> new Object());
    }

    // Mismo bloqueo para web/app/WhatsApp y entre réplicas del backend.
    private void lockClient(Integer clientId) {
        if (primaryEm.createNativeQuery("select id from clients where id = :client for update", Integer.class)
                .setParameter("client", clientId).getResultList().isEmpty()) throw ApiException.notFound("No se encontró el cliente solicitado.");
    }
}
