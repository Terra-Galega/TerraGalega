package com.example.demo.service;

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

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository repository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AddOnRepository addOnRepository;

    @Override
    @Transactional
    public Collection<Category> getAllCategorys() {
        return repository.findAll();
    }

    @Override
    @Transactional
    public Category getCategoryById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encuentran categorias con este id."));
    }

    @Override
    @Transactional
    public Category getCategoryByName(String name) {
        return repository.findByName(name);
    }

    @Override
    @Transactional
    public List<AddOn> getAddonsByCategory(String categoryName) {

        Category category = repository.findByName(categoryName);

        if (category == null) {
            throw new IllegalArgumentException("No se encontró la categoría solicitada.");
        }

        return new ArrayList<>(category.getAddOns());
    }

    // Validaciones compartidas por addCategory() y updateCategory()
    private void validateCategory(Category category, Integer currentId) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre de la categoría no puede estar vacío.");
        }

        Category sameName = repository.findByName(category.getName().trim());

        if (sameName != null && !sameName.getId().equals(currentId)) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre.");
        }
    }

    @Override
    @Transactional
    public Category addCategory(Category category) {

        validateCategory(category, null);

        Category nueva = new Category();
        nueva.setName(category.getName().trim());
        nueva.setDescription(category.getDescription());

        return repository.save(nueva);
    }

    @Override
    @Transactional
    public Category updateCategory(Integer id, Category category) {

        Category existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La categoría no existe."));

        validateCategory(category, id);

        // Solo se actualizan los datos propios; la relación con adicionales
        // se maneja desde AddOn (lado dueño).
        existing.setName(category.getName().trim());
        existing.setDescription(category.getDescription());

        return repository.save(existing);
    }

    @Override
    @Transactional
    public void deleteCategory(Integer id) {

        Category category = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la categoría que se desea eliminar."));

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
        addOnRepository.saveAll(addOns);
        addOnRepository.flush();

        repository.delete(category);
    }
}