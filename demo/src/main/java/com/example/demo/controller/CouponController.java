package com.example.demo.controller;

import com.example.demo.dto.ClientCouponDTO;
import com.example.demo.dto.CouponDiscountDTO;
import com.example.demo.entities.Client;
import com.example.demo.entities.Coupon;
import com.example.demo.security.AuthenticatedUser;
import com.example.demo.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/coupons")
// Permite las peticiones del front de Angular que corre en localhost:4200
@CrossOrigin(origins = "http://localhost:4200")
public class CouponController {

    // La lógica de negocio (validaciones y reglas) vive en el servicio
    @Autowired
    private CouponService couponService;

    // Cliente que hace la petición, obtenido del token
    @Autowired
    private AuthenticatedUser authenticatedUser;

    // GET /coupons -> todos los cupones (activos e inactivos), para el admin
    @Operation(summary = "Get all coupons")
    @GetMapping
    public Collection<Coupon> getAllCoupons() {
        return couponService.getAllCoupons();
    }

    // GET /coupons/me -> cupones (personales y generales) que el cliente puede
    // usar ahora, para el selector del checkout
    @Operation(summary = "Get the coupons the logged-in client can use right now")
    @GetMapping("/me")
    public Collection<Coupon> getMyAvailableCoupons() {
        return couponService.getAvailableCouponsByClient(authenticatedUser.client().getId());
    }

    // GET /coupons/me/all -> todos los cupones del cliente con su estado
    // (activo, vencido, usado, agotado), para "Mis cupones"
    @Operation(summary = "Get all coupons of the logged-in client with their status")
    @GetMapping("/me/all")
    public Collection<ClientCouponDTO> getAllMyCoupons() {
        return couponService.getAllCouponsByClient(authenticatedUser.client().getId());
    }

    // GET /coupons/validate?code=X&subtotal=Z -> cuánto descontaría el cupón.
    // Es solo para mostrarlo antes de pagar: el checkout vuelve a validar
    // y a calcular todo en el servidor, sin confiar en este subtotal.
    @Operation(summary = "Validate a coupon for the logged-in client and preview its discount")
    @GetMapping("/validate")
    public CouponDiscountDTO validateCoupon(
            @RequestParam String code,
            @RequestParam Double subtotal) {

        Client client = authenticatedUser.client();
        Coupon coupon = couponService.findValidCoupon(code, client, subtotal);

        CouponDiscountDTO dto = new CouponDiscountDTO();
        dto.setCode(coupon.getCode());
        dto.setDescription(coupon.getDescription());
        dto.setDiscount(couponService.calculateDiscount(coupon, subtotal));
        return dto;
    }

    // GET /coupons/{id} -> un cupón por su id
    @Operation(summary = "Get coupon by ID")
    @GetMapping("/{id}")
    public Coupon getCouponById(@PathVariable Integer id) {
        return couponService.getCouponById(id);
    }

    // POST /coupons -> crea un cupón nuevo (el servicio valida los datos)
    @Operation(summary = "Add a new coupon")
    @PostMapping
    public Coupon addCoupon(@RequestBody Coupon coupon) {
        return couponService.addCoupon(coupon);
    }

    // PUT /coupons/{id} -> actualiza un cupón existente
    @Operation(summary = "Update a coupon")
    @PutMapping("/{id}")
    public Coupon updateCoupon(@PathVariable Integer id, @RequestBody Coupon coupon) {
        return couponService.updateCoupon(id, coupon);
    }

    // DELETE /coupons/{id} -> elimina un cupón
    @Operation(summary = "Delete a coupon")
    @DeleteMapping("/{id}")
    public void deleteCoupon(@PathVariable Integer id) {
        couponService.deleteCoupon(id);
    }

    // PUT /coupons/{id}/toggle -> alterna el cupón entre activo e inactivo
    @Operation(summary = "Toggle coupon active status")
    @PutMapping("/{id}/toggle")
    public Coupon toggleCoupon(@PathVariable Integer id) {
        return couponService.toggleCouponActive(id);
    }
}