package com.example.demo.service;

import com.example.demo.dto.ClientCouponDTO;
import com.example.demo.entities.Client;
import com.example.demo.entities.Coupon;
import java.util.Collection;

public interface CouponService {

    // Todos los cupones, activos e inactivos (vista del admin)
    Collection<Coupon> getAllCoupons();

    // Un cupón por id; falla si no existe
    Coupon getCouponById(Integer id);

    // Crea un cupón validando sus datos
    Coupon addCoupon(Coupon coupon);

    // Actualiza un cupón existente
    Coupon updateCoupon(Integer id, Coupon coupon);

    // Elimina un cupón
    void deleteCoupon(Integer id);

    // Busca el cupón por su código y comprueba que el cliente pueda usarlo en un
    // pedido con ese subtotal. Lanza un error con el motivo si no se puede.
    Coupon findValidCoupon(String code, Client client, Double subtotal);

    // Calcula cuánto descuenta el cupón sobre un subtotal
    Double calculateDiscount(Coupon coupon, Double subtotal);

    // Al eliminar una cuenta: borra los cupones personales del cliente; los que
    // ya están en pedidos se conservan sin dueño y desactivados
    void removeClientCoupons(Integer clientId);

    // Crea el cupón de bienvenida de un cliente recién registrado
    Coupon createWelcomeCoupon(Client client);

    // Cupones (personales y generales) que el cliente puede usar ahora mismo:
    // activos, vigentes, con usos disponibles y que él no haya usado
    Collection<Coupon> getAvailableCouponsByClient(Integer clientId);

    // Cupones (personales y generales) que el cliente debe ver en "Mis cupones",
    // cada uno con su estado: activo, vencido, usado o agotado
    Collection<ClientCouponDTO> getAllCouponsByClient(Integer clientId);

    // Alterna el cupón entre activo e inactivo
    Coupon toggleCouponActive(Integer id);
}