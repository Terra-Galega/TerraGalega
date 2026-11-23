package com.example.demo.whatsapp;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.*;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.example.demo.dto.CheckoutItemDTO;
import com.example.demo.entities.Product;
import com.example.demo.errors.ApiException;
import com.example.demo.movil.dto.CartDTOs.CartItemDTO;
import com.example.demo.movil.entity.WhatsAppCartAction;
import com.example.demo.movil.service.CartService;
import com.example.demo.repository.ProductRepository;
import tools.jackson.databind.ObjectMapper;

@Service
public class WhatsAppCartService {
    record Context(String messageId, String phone, WhatsAppAccountService.Account account) { }
    record Reply(String text, String actionId) {
        static Reply text(String text) { return new Reply(text, null); }
    }
    private final WhatsAppAccountService accounts;
    private final CartService cart;
    private final ProductRepository products;
    private final EntityManager pg;
    private final TransactionTemplate pgTx;
    private final ObjectMapper mapper;

    public WhatsAppCartService(WhatsAppAccountService accounts, CartService cart, ProductRepository products,
            @Qualifier("postgresEntityManagerFactory") EntityManagerFactory factory,
            @Qualifier("postgresTransactionManager") PlatformTransactionManager manager, ObjectMapper mapper) {
        this.accounts = accounts; this.cart = cart; this.products = products; this.mapper = mapper;
        pg = SharedEntityManagerCreator.createSharedEntityManager(factory);
        pgTx = new TransactionTemplate(manager);
    }

    String summary(Context context) {
        return accounts.withAccount(context.phone(), context.account(), () -> summary(cart.getCart(context.account().clientId())));
    }

    Map<String, Object> inspect(Context context) {
        return accounts.withAccount(context.phone(), context.account(), () -> {
            var lines = cart.getCart(context.account().clientId());
            return Map.of("lineas", lines.stream().map(line -> Map.of("lineaId", line.id(), "productoId", line.product().id(),
                    "plato", line.product().name(), "cantidad", line.quantity(), "adicionales", line.addOns())).toList(), "resumen", summary(lines));
        });
    }

    Reply prepare(Context context, Map<String, Object> args) {
        return accounts.withAccount(context.phone(), context.account(), () -> pgTx.execute(status -> {
            String id = digest(context.messageId());
            var existing = pg.find(WhatsAppCartAction.class, id, LockModeType.PESSIMISTIC_WRITE);
            if (existing != null) { checkOwner(existing, context); return render(existing); }
            String operation = String.valueOf(args.getOrDefault("accion", ""));
            if (!List.of("agregar", "cantidad", "quitar").contains(operation)) throw ApiException.badRequest("Pide agregar un plato, cambiar su cantidad o quitar una línea del carrito.");
            int quantity = "quitar".equals(operation) ? 0 : integer(args.get("cantidad"));
            if (!"quitar".equals(operation) && (quantity < 1 || quantity > 99)) throw ApiException.badRequest("Indica una cantidad entera entre 1 y 99. Para quitar un plato, pide quitarlo del carrito.");
            var lines = cart.getCart(context.account().clientId());
            var action = new WhatsAppCartAction();
            action.setId(id); action.setPhone(context.phone()); action.setClientId(context.account().clientId());
            action.setLinkedAt(context.account().linkedAt()); action.setOperation(operation); action.setQuantity(quantity);
            action.setCartHash(fingerprint(lines)); action.setState("PENDING"); action.setCreatedAt(Instant.now());
            action.setExpiresAt(Instant.now().plusSeconds(600));
            if ("agregar".equals(operation)) {
                int productId = integer(args.get("productoId"));
                Product product = activeProduct(productId);
                action.setProductId(productId); action.setUnitPrice(product.getPrice());
                int current = lines.stream().filter(l -> l.id().equals(productId + ":")).mapToInt(CartItemDTO::quantity).sum();
                if (current + quantity > 99) throw ApiException.badRequest("El cambio superaría las 99 unidades de ese plato en el carrito.");
                action.setDescription("Agregar " + quantity + " × " + shortName(product.getName()) + " (sin adicionales), por " + money(product.getPrice() * quantity) + " COP.");
            } else {
                String lineId = String.valueOf(args.getOrDefault("lineaId", ""));
                var line = lines.stream().filter(l -> l.id().equals(lineId)).findFirst()
                        .orElseThrow(() -> ApiException.badRequest("Ese plato o combinación no aparece en tu carrito. Pide ver el carrito y elige una línea concreta."));
                action.setLineId(lineId);
                if ("cantidad".equals(operation) && !Boolean.TRUE.equals(line.product().active())) throw ApiException.conflict("Ese plato ya no está disponible; puedes quitarlo del carrito.");
                action.setDescription("quitar".equals(operation) ? "Quitar del carrito " + name(line) + " (" + line.quantity() + " unidades)."
                        : "Cambiar " + name(line) + " de " + line.quantity() + " a " + quantity + " unidades en total.");
            }
            // Una propuesta nueva sustituye las anteriores; no se ejecuta nada todavía.
            pg.createQuery("update WhatsAppCartAction a set a.state = 'CANCELLED' where a.phone = :phone and a.state = 'PENDING'")
                    .setParameter("phone", context.phone()).executeUpdate();
            pg.persist(action);
            return render(action);
        }));
    }

    Reply decide(Context context, String actionId, boolean confirm) {
        return accounts.withAccount(context.phone(), context.account(), () -> pgTx.execute(status -> {
            WhatsAppCartAction action;
            if (actionId == null || actionId.isBlank()) {
                action = pg.createQuery("select a from WhatsAppCartAction a where a.phone = :phone and a.clientId = :client and a.linkedAt = :linked and a.state = 'PENDING' order by a.createdAt desc", WhatsAppCartAction.class)
                        .setParameter("phone", context.phone()).setParameter("client", context.account().clientId())
                        .setParameter("linked", context.account().linkedAt()).setMaxResults(1).setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .getResultStream().findFirst().orElse(null);
            } else {
                if (!actionId.matches("[a-f0-9]{64}")) return Reply.text("Ese cambio no está disponible. Pide de nuevo el cambio del carrito.");
                action = pg.find(WhatsAppCartAction.class, actionId, LockModeType.PESSIMISTIC_WRITE);
            }
            if (action == null) return Reply.text("No tienes un cambio de carrito pendiente. Dime qué plato quieres agregar, cambiar o quitar.");
            pg.refresh(action, LockModeType.PESSIMISTIC_WRITE);
            checkOwner(action, context);
            if (!"PENDING".equals(action.getState())) return render(action);
            if (!action.getExpiresAt().isAfter(Instant.now())) {
                action.setState("EXPIRED"); return render(action);
            }
            if (!confirm) { action.setState("CANCELLED"); return render(action); }
            var before = cart.getCart(context.account().clientId());
            if (!action.getCartHash().equals(fingerprint(before))) {
                action.setState("CHANGED"); return render(action);
            }
            List<CartItemDTO> after;
            if ("agregar".equals(action.getOperation())) {
                Product product = activeProduct(action.getProductId());
                if (!Objects.equals(action.getUnitPrice(), product.getPrice())) { action.setState("CHANGED"); return render(action); }
                var item = new CheckoutItemDTO(); item.setProductId(action.getProductId());
                item.setQuantity(action.getQuantity()); item.setAddOnIds(List.of());
                after = cart.addItem(context.account().clientId(), item);
            } else if ("cantidad".equals(action.getOperation())) {
                after = cart.setQuantity(context.account().clientId(), action.getLineId(), action.getQuantity());
            } else {
                after = cart.removeItem(context.account().clientId(), action.getLineId());
            }
            // CartService se une a esta transacción PG: cambio y comprobante son atómicos.
            action.setState("APPLIED"); action.setResult("✅ Cambio confirmado.\n" + summary(after));
            pg.flush();
            return Reply.text(action.getResult());
        }));
    }

    private void checkOwner(WhatsAppCartAction action, Context context) {
        if (!action.getPhone().equals(context.phone()) || !action.getClientId().equals(context.account().clientId())
                || !action.getLinkedAt().equals(context.account().linkedAt())) throw ApiException.forbidden("Ese cambio no pertenece a tu vinculación actual. Pide uno nuevo.");
    }

    private Reply render(WhatsAppCartAction action) {
        return switch (action.getState()) {
            case "PENDING" -> action.getExpiresAt().isAfter(Instant.now())
                    ? new Reply("🛒 " + action.getDescription() + "\n¿Confirmas este cambio? Vence en 10 minutos. Usa los botones o escribe «confirmar carrito» / «cancelar cambio». No se realiza ningún pago.", action.getId())
                    : Reply.text("La propuesta de carrito venció. Pide de nuevo el cambio.");
            case "APPLIED" -> Reply.text("Este cambio ya fue aplicado; no se repitió.\n" + action.getResult());
            case "CHANGED" -> Reply.text("El carrito o los precios cambiaron desde la propuesta. No apliqué el cambio. Escribe «mi carrito», revísalo y pide el cambio de nuevo.");
            case "EXPIRED" -> Reply.text("La propuesta de carrito venció. Pide de nuevo el cambio.");
            default -> Reply.text("El cambio fue cancelado o sustituido. Tu carrito no se modificó por esta propuesta.");
        };
    }

    private Product activeProduct(Integer id) {
        var product = products.findById(id).orElseThrow(() -> ApiException.conflict("Ese plato ya no existe en el menú."));
        if (!Boolean.TRUE.equals(product.getActive()) || product.getPrice() == null || !Double.isFinite(product.getPrice()) || product.getPrice() < 0) throw ApiException.conflict("Ese plato ya no está disponible.");
        return product;
    }

    private String fingerprint(List<CartItemDTO> lines) {
        return digest(mapper.writeValueAsString(lines.stream().sorted(Comparator.comparing(CartItemDTO::id)).toList()));
    }

    static String digest(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private static int integer(Object value) {
        try { return new BigDecimal(String.valueOf(value)).intValueExact(); }
        catch (RuntimeException invalid) { throw ApiException.badRequest("Indica el plato y una cantidad entera válida."); }
    }

    private static String money(double value) { return NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO")).format(value); }
    private static String shortName(String value) { return value.length() > 120 ? value.substring(0, 119) + "…" : value; }
    private static String name(CartItemDTO line) {
        String extras = line.addOns().isEmpty() ? "" : " con " + line.addOns().stream().map(a -> a.name()).reduce((a, b) -> a + ", " + b).orElse("");
        return shortName(line.product().name() + extras);
    }

    private static String summary(List<CartItemDTO> lines) {
        if (lines.isEmpty()) return "🛒 Tu carrito está vacío. Dime qué plato del menú quieres agregar.";
        var text = new StringBuilder("🛒 Tu carrito:\n"); double total = 0; int count = 0;
        for (var line : lines) {
            double unit = line.product().price() + line.addOns().stream().mapToDouble(a -> a.price()).sum();
            total += unit * line.quantity();
            if (count++ < 12) text.append("• ").append(line.quantity()).append(" × ").append(name(line)).append(" — ")
                    .append(money(unit * line.quantity())).append(" COP").append(Boolean.TRUE.equals(line.product().active()) ? "" : " (no disponible)").append('\n');
        }
        if (lines.size() > 12) text.append("…y ").append(lines.size() - 12).append(" líneas más; puedes verlas en la web.\n");
        return text.append("Subtotal de productos: ").append(money(total)).append(" COP.\nPuedes agregar platos, cambiar cantidades o quitar una línea. El envío y el pago se revisan en la web.").toString();
    }
}
