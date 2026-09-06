package com.example.demo.errors;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Integer id) {
        super("No se encuentra un producto con el id " + id);
    }

}
