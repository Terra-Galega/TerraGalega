package com.example.demo.errors;

import java.util.List;

public class CategoryHasProductsException extends RuntimeException {

    private final List<String> products;

    public CategoryHasProductsException(List<String> products) {
        super("No se puede eliminar esta categoría porque tiene los siguientes productos asociados:");
        this.products = products;
    }

    public List<String> getProducts() {
        return products;
    }
}