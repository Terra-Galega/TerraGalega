package com.example.demo.errors;

import java.util.List;

// Excepción  que se usa cuando se intenta eliminar una categoría
// que todavía tiene productos asociados.
public class CategoryHasProductsException extends RuntimeException {

    // Lista con los nombres de los productos asociados a la categoría.
    private final List<String> products;

    // Construye la excepción con la lista de productos que impiden eliminar la categoría.
    public CategoryHasProductsException(List<String> products) {
        // Define el mensaje principal de la excepción.
        super("No se puede eliminar esta categoría porque tiene los siguientes productos asociados:");
        this.products = products;
    }

    // Devuelve la lista de productos asociados a la categoría.
    public List<String> getProducts() {
        return products;
    }
}