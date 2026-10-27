package com.example.demo.errors;

// Excepción personalizada que representa un producto que no se encuentra disponible.
public class ProductNotFoundException extends RuntimeException {

    // Construye la excepción cuando no se encuentra un producto identificado por su ID.
    public ProductNotFoundException(Integer id) {
        // Define el mensaje que se mostrará cuando el producto ya no esté disponible.
        super("Este producto ya no está disponible.");
    }

    // Construye la excepción utilizando un mensaje personalizado.
    public ProductNotFoundException(String message) {
        super(message);
    }

}
