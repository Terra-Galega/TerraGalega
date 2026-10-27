package com.example.demo.service;

import com.example.demo.entities.Category;
import com.example.demo.entities.AddOn;
import java.util.Collection;
import java.util.List;

/**
 * Contrato de la lógica de negocio de las categorías.
 * La implementación está en CategoryServiceImpl.
 */
public interface CategoryService {
    // Todas las categorías
    Collection<Category> getAllCategorys();

    // Una categoría por id; falla si no existe
    Category getCategoryById(Integer id);

    // Una categoría por su nombre (devuelve null si no existe)
    Category getCategoryByName(String name);

    // Adicionales asociados a la categoría con ese nombre
    List<AddOn> getAddonsByCategory(String categoryName);

    // Crea una categoría nueva (el nombre no se puede repetir)
    Category addCategory(Category category);

    // Actualiza el nombre y la descripción de una categoría
    Category updateCategory(Integer id, Category category);

    // Elimina una categoría (no se permite si tiene productos)
    void deleteCategory(Integer id);
}