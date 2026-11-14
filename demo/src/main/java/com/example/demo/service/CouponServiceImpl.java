package com.example.demo.service;

import com.example.demo.dto.ClientCouponDTO;
import com.example.demo.dto.ClientCouponStatus;
import com.example.demo.entities.Client;
import com.example.demo.entities.Coupon;
import com.example.demo.entities.DiscountType;
import com.example.demo.entities.OrderStatus;
import com.example.demo.errors.ApiException;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.CouponRepository;
import com.example.demo.repository.OrderRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
public class CouponServiceImpl implements CouponService {
    // Descuento de bienvenida: porcentaje y días de vigencia
    private static final double WELCOME_PERCENTAGE = 10.0;
    private static final int WELCOME_DAYS = 30;

    // Límites de los campos del cupón (coinciden con el front)
    private static final double MAX_AMOUNT = 10_000_000;
    private static final int MAX_USES = 1_000_000;
    private static final int DESCRIPTION_MAX = 255;
    private static final String CODE_PATTERN = "^[A-Za-z0-9_-]{3,40}$";

    // Acceso a la tabla de cupones
    @Autowired
    private CouponRepository repository;

    // Para cargar el cliente dueño de un cupón personal
    @Autowired
    private ClientRepository clientRepository;

    // Para saber en qué pedidos se ha usado un cupón
    @Autowired
    private OrderRepository orderRepository;

    @Override
    @Transactional
    public Collection<Coupon> getAllCoupons() {
        return repository.findAll();
    }

    @Override
    @Transactional
    public Coupon getCouponById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("No se encontró el cupón solicitado."));
    }

    // Validaciones compartidas por addCoupon() y updateCoupon()
    private void validateCoupon(Coupon coupon) {
        if (coupon.getCode() == null || coupon.getCode().isBlank()) {
            throw ApiException.badRequest("El código del cupón no puede estar vacío.");
        }
        if (!coupon.getCode().trim().matches(CODE_PATTERN)) {
            throw ApiException.badRequest(
                    "El código debe tener entre 3 y 40 caracteres y solo puede incluir letras, números, guion (-) y guion bajo (_), sin espacios.");
        }
        if (coupon.getDescription() != null && coupon.getDescription().trim().length() > DESCRIPTION_MAX) {
            throw ApiException.badRequest("La descripción no puede tener más de " + DESCRIPTION_MAX + " caracteres.");
        }
        if (coupon.getDiscountType() == null) {
            throw ApiException.badRequest("Indica el tipo de descuento.");
        }
        if (coupon.getDiscountValue() == null || coupon.getDiscountValue() <= 0) {
            throw ApiException.badRequest("El valor del descuento debe ser mayor que 0.");
        }
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE && coupon.getDiscountValue() > 100) {
            throw ApiException.badRequest("El porcentaje no puede ser mayor que 100.");
        }
        if (coupon.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            if (coupon.getDiscountValue() % 1 != 0) {
                throw ApiException.badRequest("El monto fijo debe ser un número entero de pesos.");
            }
            if (coupon.getDiscountValue() > MAX_AMOUNT) {
                throw ApiException.badRequest("El monto fijo no puede superar $" + (long) MAX_AMOUNT + ".");
            }
        }
        if (coupon.getMaxDiscount() != null
                && (coupon.getMaxDiscount() <= 0 || coupon.getMaxDiscount() > MAX_AMOUNT)) {
            throw ApiException.badRequest("El tope del descuento debe estar entre 1 y " + (long) MAX_AMOUNT + ".");
        }
        if (coupon.getMinOrderAmount() != null
                && (coupon.getMinOrderAmount() < 0 || coupon.getMinOrderAmount() > MAX_AMOUNT)) {
            throw ApiException.badRequest("El monto mínimo debe estar entre 0 y " + (long) MAX_AMOUNT + ".");
        }
        if (coupon.getMaxUses() != null && (coupon.getMaxUses() < 1 || coupon.getMaxUses() > MAX_USES)) {
            throw ApiException.badRequest("Los usos máximos deben estar entre 1 y " + MAX_USES + ".");
        }
        if (coupon.getStartsAt() != null && coupon.getExpiresAt() != null
                && !coupon.getExpiresAt().isAfter(coupon.getStartsAt())) {
            throw ApiException.badRequest("La fecha de vencimiento debe ser posterior a la de inicio.");
        }
    }

    // El front solo manda el id del cliente; aquí se carga el real desde la
    // base de datos. Sin cliente, el cupón queda como general.
    private Client resolveClient(Client incoming) {
        if (incoming == null) {
            return null;
        }
        if (incoming.getId() == null) {
            throw ApiException.badRequest("Cliente inválido.");
        }
        return clientRepository.findById(incoming.getId())
                .orElseThrow(() -> ApiException.badRequest(
                        "El cliente con id " + incoming.getId() + " no existe."));
    }

    // Crea un cupón nuevo. Se arma una entidad limpia (en vez de guardar la
    // recibida) para que el front no pueda forzar un id.
    @Override
    @Transactional
    public Coupon addCoupon(Coupon coupon) {

        validateCoupon(coupon);

        // Un cupón nuevo no puede nacer ya vencido
        if (coupon.getExpiresAt() != null && !coupon.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("La fecha de vencimiento debe ser futura.");
        }

        // El código se guarda siempre sin espacios y en mayúsculas
        String code = coupon.getCode().trim().toUpperCase();

        if (repository.findByCode(code) != null) {
            throw ApiException.conflict("Ya existe un cupón con ese código.");
        }

        Coupon newCoupon = new Coupon();
        newCoupon.setCode(code);
        newCoupon.setDescription(coupon.getDescription());
        newCoupon.setDiscountType(coupon.getDiscountType());
        newCoupon.setDiscountValue(coupon.getDiscountValue());
        newCoupon.setMinOrderAmount(coupon.getMinOrderAmount());
        newCoupon.setStartsAt(coupon.getStartsAt());
        newCoupon.setExpiresAt(coupon.getExpiresAt());
        newCoupon.setMaxUses(coupon.getMaxUses());
        newCoupon.setClient(resolveClient(coupon.getClient()));

        // El tope solo tiene sentido en los cupones de porcentaje
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            newCoupon.setMaxDiscount(coupon.getMaxDiscount());
        }

        // Si no se indica el estado, el cupón nace activo
        newCoupon.setActive(coupon.getActive() == null ? true : coupon.getActive());

        return repository.save(newCoupon);
    }

    // Actualiza un cupón existente. Se modifica la entidad guardada en vez de
    // reemplazarla.
    @Override
    @Transactional
    public Coupon updateCoupon(Integer id, Coupon coupon) {

        Coupon existing = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("El cupón no existe."));

        validateCoupon(coupon);

        // Un vencimiento pasado solo se rechaza si cambió; así se puede seguir
        // editando o desactivando un cupón que ya venció
        LocalDateTime currentExpires = existing.getExpiresAt() == null
                ? null
                : existing.getExpiresAt().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime newExpires = coupon.getExpiresAt() == null
                ? null
                : coupon.getExpiresAt().truncatedTo(ChronoUnit.MINUTES);
        if (newExpires != null && !Objects.equals(newExpires, currentExpires)
                && !newExpires.isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("La fecha de vencimiento debe ser futura.");
        }

        String code = coupon.getCode().trim().toUpperCase();

        // El código no puede ser el de otro cupón
        Coupon sameCode = repository.findByCode(code);
        if (sameCode != null && !sameCode.getId().equals(id)) {
            throw ApiException.conflict("Ya existe otro cupón con ese código.");
        }

        existing.setCode(code);
        existing.setDescription(coupon.getDescription());
        existing.setDiscountType(coupon.getDiscountType());
        existing.setDiscountValue(coupon.getDiscountValue());
        existing.setMinOrderAmount(coupon.getMinOrderAmount());
        existing.setStartsAt(coupon.getStartsAt());
        existing.setExpiresAt(coupon.getExpiresAt());
        existing.setMaxUses(coupon.getMaxUses());
        existing.setClient(resolveClient(coupon.getClient()));

        // El tope solo tiene sentido en los cupones de porcentaje
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            existing.setMaxDiscount(coupon.getMaxDiscount());
        } else {
            existing.setMaxDiscount(null);
        }

        // Si no viene el estado, se conserva el que ya tenía
        if (coupon.getActive() != null) {
            existing.setActive(coupon.getActive());
        }

        return repository.save(existing);
    }

    // Si el cupón ya está en algún pedido no se puede borrar, porque dejaría el
    // historial incompleto; en ese caso se debe desactivar.
    @Override
    @Transactional
    public void deleteCoupon(Integer id) {

        if (!repository.existsById(id)) {
            throw ApiException.notFound("No se encontró el cupón que se desea eliminar.");
        }

        if (orderRepository.existsByCouponId(id)) {
            throw ApiException.conflict(
                    "No se puede eliminar este cupón porque ya fue usado en pedidos. Puedes desactivarlo.");
        }

        repository.deleteById(id);
    }

    // Comprueba, una por una, las condiciones para usar un cupón en el checkout
    @Override
    @Transactional
    public Coupon findValidCoupon(String code, Client client, Double subtotal) {

        Coupon coupon = repository.findByCode(code.trim().toUpperCase());

        if (coupon == null) {
            throw ApiException.notFound("El cupón no existe.");
        }

        if (!Boolean.TRUE.equals(coupon.getActive())) {
            throw ApiException.conflict("Este cupón no está activo.");
        }

        LocalDateTime now = LocalDateTime.now();

        if (coupon.getStartsAt() != null && now.isBefore(coupon.getStartsAt())) {
            throw ApiException.conflict("Este cupón aún no está vigente.");
        }

        if (coupon.getExpiresAt() != null && now.isAfter(coupon.getExpiresAt())) {
            throw ApiException.conflict("Este cupón ya venció.");
        }

        // Un cupón personal solo lo puede usar su dueño
        if (coupon.getClient() != null && !coupon.getClient().getId().equals(client.getId())) {
            throw ApiException.forbidden("Este cupón no te pertenece.");
        }

        // Los pedidos cancelados no cuentan como uso
        if (coupon.getMaxUses() != null
                && orderRepository.countByCouponIdAndStatusNot(
                        coupon.getId(), OrderStatus.CANCELLED) >= coupon.getMaxUses()) {
            throw ApiException.conflict("Este cupón ya alcanzó su límite de usos.");
        }

        // Cada cliente usa un cupón una sola vez
        if (orderRepository.existsByCouponIdAndClientIdAndStatusNot(
                coupon.getId(), client.getId(), OrderStatus.CANCELLED)) {
            throw ApiException.conflict("Ya usaste este cupón.");
        }

        if (coupon.getMinOrderAmount() != null && subtotal < coupon.getMinOrderAmount()) {
            throw ApiException.conflict("El pedido debe tener un subtotal mínimo de $"
                    + coupon.getMinOrderAmount().longValue() + " para usar este cupón.");
        }

        return coupon;
    }

    // Porcentaje (con tope opcional) o monto fijo; nunca descuenta más que el subtotal
    @Override
    public Double calculateDiscount(Coupon coupon, Double subtotal) {

        double discount;

        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = subtotal * coupon.getDiscountValue() / 100.0;
            if (coupon.getMaxDiscount() != null && discount > coupon.getMaxDiscount()) {
                discount = coupon.getMaxDiscount();
            }
        } else {
            discount = coupon.getDiscountValue();
        }

        if (discount > subtotal) {
            discount = subtotal;
        }

        // Pesos sin decimales
        return (double) Math.round(discount);
    }

    // Los cupones ya usados no se pueden borrar (los pedidos los referencian),
    // así que se desactivan y quedan sin dueño.
    @Override
    @Transactional
    public void removeClientCoupons(Integer clientId) {

        List<Coupon> coupons = repository.findByClientId(clientId);

        for (Coupon coupon : coupons) {
            if (orderRepository.existsByCouponId(coupon.getId())) {
                coupon.setClient(null);
                coupon.setActive(false);
                repository.save(coupon);
            } else {
                repository.delete(coupon);
            }
        }
    }

    // Cupón personal de un solo uso para un cliente recién registrado. El código
    // lleva el id del cliente, así que nunca se repite.
    @Override
    @Transactional
    public Coupon createWelcomeCoupon(Client client) {

        Coupon coupon = new Coupon();
        coupon.setCode("BIENVENIDA-" + client.getId());
        coupon.setDescription("Descuento de bienvenida");
        coupon.setDiscountType(DiscountType.PERCENTAGE);
        coupon.setDiscountValue(WELCOME_PERCENTAGE);
        coupon.setExpiresAt(LocalDateTime.now().plusDays(WELCOME_DAYS));
        coupon.setMaxUses(1);
        coupon.setActive(true);
        coupon.setClient(client);

        return repository.save(coupon);
    }

    // Los mismos requisitos de findValidCoupon, pero sin lanzar errores: los
    // cupones que no se pueden usar simplemente no aparecen en la lista.
    // Incluye los personales del cliente y los generales.
    @Override
    @Transactional
    public Collection<Coupon> getAvailableCouponsByClient(Integer clientId) {

        if (!clientRepository.existsById(clientId)) {
            throw ApiException.notFound("No se encontró el cliente solicitado.");
        }

        LocalDateTime now = LocalDateTime.now();
        List<Coupon> available = new ArrayList<>();

        for (Coupon coupon : repository.findForClient(clientId)) {

            if (!Boolean.TRUE.equals(coupon.getActive())) {
                continue;
            }
            if (coupon.getStartsAt() != null && now.isBefore(coupon.getStartsAt())) {
                continue;
            }
            if (coupon.getExpiresAt() != null && now.isAfter(coupon.getExpiresAt())) {
                continue;
            }
            if (coupon.getMaxUses() != null
                    && orderRepository.countByCouponIdAndStatusNot(
                            coupon.getId(), OrderStatus.CANCELLED) >= coupon.getMaxUses()) {
                continue;
            }
            if (orderRepository.existsByCouponIdAndClientIdAndStatusNot(
                    coupon.getId(), clientId, OrderStatus.CANCELLED)) {
                continue;
            }

            available.add(coupon);
        }

        return available;
    }

    // Cupones del cliente (personales y generales) con su estado, para
    // "Mis cupones". Los desactivados por el admin y los que aún no empiezan
    // no se muestran.
    @Override
    @Transactional
    public Collection<ClientCouponDTO> getAllCouponsByClient(Integer clientId) {

        if (!clientRepository.existsById(clientId)) {
            throw ApiException.notFound("No se encontró el cliente solicitado.");
        }

        LocalDateTime now = LocalDateTime.now();
        List<ClientCouponDTO> result = new ArrayList<>();

        for (Coupon coupon : repository.findForClient(clientId)) {

            if (!Boolean.TRUE.equals(coupon.getActive())) {
                continue;
            }
            if (coupon.getStartsAt() != null && now.isBefore(coupon.getStartsAt())) {
                continue;
            }

            // Orden de prioridad: usado > vencido > agotado > activo
            ClientCouponStatus status;

            if (orderRepository.existsByCouponIdAndClientIdAndStatusNot(
                    coupon.getId(), clientId, OrderStatus.CANCELLED)) {
                status = ClientCouponStatus.USED;
            } else if (coupon.getExpiresAt() != null && now.isAfter(coupon.getExpiresAt())) {
                status = ClientCouponStatus.EXPIRED;
            } else if (coupon.getMaxUses() != null
                    && orderRepository.countByCouponIdAndStatusNot(
                            coupon.getId(), OrderStatus.CANCELLED) >= coupon.getMaxUses()) {
                status = ClientCouponStatus.EXHAUSTED;
            } else {
                status = ClientCouponStatus.ACTIVE;
            }

            ClientCouponDTO dto = new ClientCouponDTO();
            dto.setId(coupon.getId());
            dto.setCode(coupon.getCode());
            dto.setDescription(coupon.getDescription());
            dto.setDiscountType(coupon.getDiscountType());
            dto.setDiscountValue(coupon.getDiscountValue());
            dto.setMaxDiscount(coupon.getMaxDiscount());
            dto.setMinOrderAmount(coupon.getMinOrderAmount());
            dto.setStartsAt(coupon.getStartsAt());
            dto.setExpiresAt(coupon.getExpiresAt());
            dto.setGeneral(coupon.getClient() == null);
            dto.setStatus(status);
            result.add(dto);
        }

        return result;
    }

    // Cambia el cupón de activo a inactivo, o al revés
    @Override
    @Transactional
    public Coupon toggleCouponActive(Integer id) {

        Coupon coupon = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("El cupón no existe."));

        coupon.setActive(!coupon.getActive());

        return repository.save(coupon);
    }
}