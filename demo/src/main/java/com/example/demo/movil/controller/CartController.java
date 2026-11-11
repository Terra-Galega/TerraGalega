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

import io.swagger.v3.oas.annotations.Operation;

// Carrito del cliente, compartido entre la web y la app. Ruta base: /cart
@RestController
@RequestMapping("/cart")
@CrossOrigin(origins = "http://localhost:4200")
public class CartController {

    @Autowired
    private CartService cartService;

    @Operation(summary = "Get the shared cart of a client")
    @GetMapping("/{clientId}")
    public List<CartItemDTO> get(@PathVariable Integer clientId) {
        return cartService.getCart(clientId);
    }

    @Operation(summary = "Add a product (with add-ons) to the cart")
    @PostMapping("/{clientId}/items")
    public List<CartItemDTO> add(@PathVariable Integer clientId, @RequestBody CheckoutItemDTO item) {
        return cartService.addItem(clientId, item);
    }

    @Operation(summary = "Add several lines at once (guest cart after login)")
    @PostMapping("/{clientId}/items/bulk")
    public List<CartItemDTO> addBulk(@PathVariable Integer clientId, @RequestBody List<CheckoutItemDTO> items) {
        return cartService.addItems(clientId, items);
    }

    @Operation(summary = "Change the quantity of a cart line (0 removes it)")
    @PutMapping("/{clientId}/items/{lineId}")
    public List<CartItemDTO> setQuantity(@PathVariable Integer clientId, @PathVariable String lineId,
            @RequestBody QuantityRequestDTO body) {
        return cartService.setQuantity(clientId, lineId, body.quantity());
    }

    @Operation(summary = "Remove a cart line")
    @DeleteMapping("/{clientId}/items/{lineId}")
    public List<CartItemDTO> remove(@PathVariable Integer clientId, @PathVariable String lineId) {
        return cartService.removeItem(clientId, lineId);
    }

    @Operation(summary = "Empty the cart")
    @DeleteMapping("/{clientId}")
    public List<CartItemDTO> clear(@PathVariable Integer clientId) {
        cartService.clear(clientId);
        return List.of();
    }
}