package com.example.demo.errors;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Integer id) {
        super("Este producto ya no está disponible.");
    }

    public ProductNotFoundException(String message) {
        super(message);
    }

}
