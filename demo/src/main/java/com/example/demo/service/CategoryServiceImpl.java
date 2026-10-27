package com.example.demo.service;

import com.example.demo.errors.ApiException;
import com.example.demo.entities.AddOn;
import com.example.demo.entities.Category;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.repository.AddOnRepository;
import com.example.demo.entities.Product;
import com.example.demo.errors.CategoryHasProductsException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Lógica de negocio de las categorías: validación del nombre único y reglas
 * para eliminar (no se puede si tiene productos; si tiene adicionales, solo
 * se les quita la categoría).
 *
 * Todos los métodos son @Transactional, así que se ejecutan dentro de una
 * transacción y se pueden leer las relaciones sin problemas.
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    // Acceso a la tabla de categorías
    @Autowired
    private CategoryRepository repository;

    // Se usa para saber si una categoría tiene productos antes de borrarla
    @Autowired
    private ProductRepository productRepository;

    // Se usa para soltar la relación con los adicionales al borrar una categoría
    @Autowired
    private AddOnRepository addOnRepository;

    // Todas las categorías
    @Override
    @Transactional
    public Collection<Category> getAllCategorys() {
        return repository.findAll();
    }

    // Busca una categoría por id; si no existe responde 404
    @Override
    @Transactional
    public Category getCategoryById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("No se encuentran categorias con este id."));
    }

    // Busca una categoría por su nombre; devuelve null si no existe
    @Override
    @Transactional
    public Category getCategoryByName(String name) {
        return repository.findByName(name);
    }

    // Adicionales de la categoría con ese nombre. Devuelve una copia de la
    // colección para no exponer la lista interna de la entidad.
    @Override
    @Transactional
    public List<AddOn> getAddonsByCategory(String categoryName) {

        Category category = repository.findByName(categoryName);

        if (category == null) {
            throw ApiException.notFound("No se encontró la categoría solicitada.");
        }

        return new ArrayList<>(category.getAddOns());
    }

    // Validaciones compartidas por addCategory() y updateCategory().
    // currentId es el id de la categoría que se edita (null al crear): permite
    // que una categoría conserve su propio nombre sin chocar consigo misma.
    private void validateCategory(Category category, Integer currentId) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw ApiException.badRequest("El nombre de la categoría no puede estar vacío.");
        }

        Category sameName = repository.findByName(category.getName().trim());

        // Si otro registro ya usa ese nombre, se rechaza
        if (sameName != null && !sameName.getId().equals(currentId)) {
            throw ApiException.conflict("Ya existe una categoría con ese nombre.");
        }
    }

    // Crea una categoría nueva. Se arma una entidad limpia para que el front
    // no pueda forzar un id ni la relación con adicionales o productos.
    @Override
    @Transactional
    public Category addCategory(Category category) {

        validateCategory(category, null);

        Category nueva = new Category();
        nueva.setName(category.getName().trim());
        nueva.setDescription(category.getDescription());

        return repository.save(nueva);
    }

    // Actualiza nombre y descripción de una categoría existente
    @Override
    @Transactional
    public Category updateCategory(Integer id, Category category) {

        Category existing = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("La categoría no existe."));

        validateCategory(category, id);

        // Solo se actualizan los datos propios; la relación con adicionales
        // se maneja desde AddOn (lado dueño).
        existing.setName(category.getName().trim());
        existing.setDescription(category.getDescription());

        return repository.save(existing);
    }

    // Elimina una categoría siguiendo dos reglas:
    // 1) Si tiene productos no se borra, y el error incluye los nombres de esos
    // productos para que el admin sepa cuáles reasignar.
    // 2) Si tiene adicionales, se le quita la categoría a cada uno y se borra.
    @Override
    @Transactional
    public void deleteCategory(Integer id) {

        Category category = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("No se encontró la categoría que se desea eliminar."));

        // Regla 1: un producto no puede quedar sin categoría
        if (productRepository.existsByCategoryId(id)) {
            List<String> productNames = productRepository.findByCategoryId(id).stream()
                    .map(Product::getName)
                    .sorted()
                    .toList();

            if (!productNames.isEmpty()) {
                throw new CategoryHasProductsException(productNames);
            }
        }

        // AddOn es el dueño de la relación: a cada adicional que tenga esta
        // categoría se le quita solo esa; sus otras categorías se mantienen.
        // Si era la única, el adicional queda sin categorías hasta que el admin las
        // asigne.
        List<AddOn> addOns = addOnRepository.findByCategories_Id(id);
        for (AddOn addOn : addOns) {
            addOn.getCategories().removeIf(c -> c.getId().equals(id));
        }
        // flush() fuerza a escribir ya esos cambios, para que la tabla intermedia
        // quede limpia antes de borrar la categoría
        addOnRepository.saveAll(addOns);
        addOnRepository.flush();

        repository.delete(category);
    }
}