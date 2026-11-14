package com.example.demo.movil.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CheckoutItemDTO;
import com.example.demo.movil.dto.CartDTOs.CartItemDTO;
import com.example.demo.movil.dto.CartDTOs.QuantityRequestDTO;
import com.example.demo.movil.service.CartService;
import com.example.demo.security.AuthenticatedUser;

import io.swagger.v3.oas.annotations.Operation;

// Carrito del cliente con sesión iniciada, compartido entre la web y la app.
@RestController
@RequestMapping("/cart/me")
@CrossOrigin(origins = "http://localhost:4200")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private AuthenticatedUser authenticatedUser;

    // Id del cliente que hace la petición, obtenido del token
    private Integer clientId() {
        return authenticatedUser.client().getId();
    }

    @Operation(summary = "Get the cart of the logged-in client")
    @GetMapping
    public List<CartItemDTO> get() {
        return cartService.getCart(clientId());
    }

    @Operation(summary = "Add a product (with add-ons) to the cart")
    @PostMapping("/items")
    public List<CartItemDTO> add(@RequestBody CheckoutItemDTO item) {
        return cartService.addItem(clientId(), item);
    }

    @Operation(summary = "Add several lines at once (guest cart after login)")
    @PostMapping("/items/bulk")
    public List<CartItemDTO> addBulk(@RequestBody List<CheckoutItemDTO> items) {
        return cartService.addItems(clientId(), items);
    }

    @Operation(summary = "Change the quantity of a cart line (0 removes it)")
    @PutMapping("/items/{lineId}")
    public List<CartItemDTO> setQuantity(@PathVariable String lineId, @RequestBody QuantityRequestDTO body) {
        return cartService.setQuantity(clientId(), lineId, body.quantity());
    }

    @Operation(summary = "Remove a cart line")
    @DeleteMapping("/items/{lineId}")
    public List<CartItemDTO> remove(@PathVariable String lineId) {
        return cartService.removeItem(clientId(), lineId);
    }

    @Operation(summary = "Empty the cart")
    @DeleteMapping
    public List<CartItemDTO> clear() {
        cartService.clear(clientId());
        return List.of();
    }
}
