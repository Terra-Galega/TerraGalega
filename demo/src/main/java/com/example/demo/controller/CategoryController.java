package com.example.demo.controller;

import com.example.demo.entities.Category;
import com.example.demo.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * Controlador REST de las categorías de los platos.
 * Expone el CRUD completo. Las reglas de negocio (nombre único, no borrar una
 * categoría con productos, soltar la relación con los adicionales al borrar)
 * viven en CategoryService.
 *
 * A diferencia de AddOnController, aquí se usa la entidad Category directamente
 * porque es pequeña (id, nombre y descripción) y sus relaciones con adicionales
 * y productos están marcadas con @JsonIgnore, así que no viajan en el JSON.
 */
@RestController
@RequestMapping("/categories")
// Permite las peticiones del front de Angular que corre en localhost:4200
@CrossOrigin(origins = "http://localhost:4200")
public class CategoryController {

    // La lógica de negocio (validaciones y reglas) vive en el servicio
    @Autowired
    private CategoryService categoryService;

    // GET /categories -> todas las categorías
    @Operation(summary = "Get all categories")
    @GetMapping
    public Collection<Category> getAllCategories() {
        return categoryService.getAllCategorys();
    }

    // GET /categories/{id} -> una categoría por su id
    @Operation(summary = "Get category by ID")
    @GetMapping("/{id}")
    public Category getCategoryById(@PathVariable Integer id) {
        return categoryService.getCategoryById(id);
    }

    // POST /categories -> crea una categoría nueva (el nombre no se puede repetir)
    @Operation(summary = "Add a new category (only for admin)")
    @PostMapping
    public Category addCategory(@RequestBody Category category) {
        return categoryService.addCategory(category);
    }

    // PUT /categories/{id} -> actualiza nombre y descripción. La relación con los
    // adicionales no se toca aquí: se maneja desde el adicional (lado dueño).
    @Operation(summary = "Update a category (only for admin)")
    @PutMapping("/{id}")
    public Category updateCategory(@PathVariable Integer id, @RequestBody Category category) {
        return categoryService.updateCategory(id, category);
    }

    // DELETE /categories/{id} -> elimina la categoría. Se rechaza si tiene
    // productos (la respuesta trae sus nombres); si tiene adicionales, solo se
    // les quita esta categoría y ellos se conservan.
    @Operation(summary = "Delete a category (only for admin)")
    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable Integer id) {
        categoryService.deleteCategory(id);
    }
}