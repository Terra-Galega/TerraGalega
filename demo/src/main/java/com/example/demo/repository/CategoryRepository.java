package com.example.demo.repository;

import com.example.demo.entities.AddOn;
import com.example.demo.entities.Category;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class CategoryRepository {

    private final Map<Integer, Category> categoryMap = new HashMap<>();

    // DB quemada
    public CategoryRepository() {
        categoryMap.put(1, new Category(1, "Entradas",
                "Pequeños platos gallegos pensados para abrir el apetito antes del plato principal.",
                List.of(
                        new AddOn(5, "Ensalada verde", "Adicional para Empanada", 5000.0, true))));

        categoryMap.put(2, new Category(2, "Mariscos",
                "Lo mejor de la costa gallega: mariscos frescos preparados con recetas tradicionales.",
                List.of(
                        new AddOn(6, "Extra ajo", "Adicional para mariscos", 2000.0, true),
                        new AddOn(7, "Salsa de limón", "Adicional para mariscos", 3000.0, true))));

        categoryMap.put(3, new Category(3, "Carnes",
                "Cortes a la brasa y guisos tradicionales de la cocina gallega.",
                List.of(
                        new AddOn(12, "Pan de millo", "Adicional para Lacón", 3500.0, true),
                        new AddOn(13, "Cachelos", "Adicional para Lacón", 6000.0, true))));

        categoryMap.put(4, new Category(4, "Postres",
                "Dulces clásicos de Galicia para cerrar la comida con sabor a tierra.",
                List.of(
                        new AddOn(16, "Nata montada", "Adicional para Tarta de Santiago", 3000.0, true),
                        new AddOn(17, "Helado de vainilla", "Adicional para Tarta de Santiago", 4000.0, true))));
    }

    // Devuelve todas las categorías
    public Collection<Category> findAll() {
        return categoryMap.values();
    }

    // Devuelve una categoría por su id
    public Category findById(Integer id) {
        return categoryMap.get(id);
    }

    // Devuelve una categoría por su nombre (así se relaciona con Product.category,
    // que sigue guardando el nombre de la categoría como texto)
    public Category findByName(String name) {
        if (name == null) {
            return null;
        }
        return categoryMap.values().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public List<AddOn> getAddonsByCategory(String categoryName) {
        Category category = findByName(categoryName);
        if (category != null) {
            return category.getAdditionals();
        }
        return new ArrayList<>();
    }
}