package com.example.demo.service;

import com.example.demo.entities.AddOn;
import com.example.demo.entities.Category;
import com.example.demo.repository.AddOnRepository;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Service
public class AddOnServiceImpl implements AddOnService {

    @Autowired
    private AddOnRepository repository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Override
    @Transactional
    public Collection<AddOn> getAllAddOns() {
        return repository.findAll();
    }

    @Override
    @Transactional
    public Collection<AddOn> getAllAddOnsActive() {
        return repository.findByActiveTrue();
    }

    @Override
    @Transactional
    public AddOn getAddOnById(Integer id) {
        return repository.findById(id)  
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el adicional solicitado."));
    }
    @Override
    @Transactional
    public Collection<AddOn> getActiveAddOnsByCategory(Integer categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new IllegalArgumentException("No se encuentran categorias con este id.");
        }
        return repository.findByActiveTrueAndCategories_Id(categoryId);
    }

    // Validaciones compartidas por addAddOn() y updateAddOn()
    private void validateAddOn(AddOn addOn) {
        if (addOn.getName() == null || addOn.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre del adicional no puede estar vacío.");
        }
        // Los adicionales pueden ser gratuitos, por eso se permite precio 0
        if (addOn.getPrice() == null || addOn.getPrice() < 0) {
            throw new IllegalArgumentException("El precio del adicional no puede ser negativo.");
        }
    }

    // El front solo manda {id} de cada categoría; aquí se cargan las reales
    private Set<Category> resolveCategories(Set<Category> incoming) {
        Set<Category> result = new HashSet<>();

        if (incoming == null) {
            return result;
        }

        for (Category c : incoming) {
            if (c.getId() == null) {
                throw new IllegalArgumentException("Categoría inválida.");
            }
            result.add(categoryRepository.findById(c.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "La categoría con id " + c.getId() + " no existe.")));
        }

        return result;
    }

    @Override
    @Transactional
    public AddOn addAddOn(AddOn addOn) {

        validateAddOn(addOn);

        AddOn nuevo = new AddOn();
        nuevo.setName(addOn.getName().trim());
        nuevo.setDescription(addOn.getDescription());
        nuevo.setPrice(addOn.getPrice());
        nuevo.setActive(addOn.getActive() == null ? true : addOn.getActive());
        nuevo.setCategories(resolveCategories(addOn.getCategories()));

        return repository.save(nuevo);
    }

    @Override
    @Transactional
    public AddOn updateAddOn(Integer id, AddOn addOn) {

        AddOn existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El adicional no existe."));

        validateAddOn(addOn);

        existing.setName(addOn.getName().trim());
        existing.setDescription(addOn.getDescription());
        existing.setPrice(addOn.getPrice());
        if (addOn.getActive() != null) {
            existing.setActive(addOn.getActive());
        }

        existing.getCategories().clear();
        existing.getCategories().addAll(resolveCategories(addOn.getCategories()));

        return repository.save(existing);
    }

    @Override
    @Transactional
    public void deleteAddOn(Integer id) {

        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("No se encontró el adicional que se desea eliminar.");
        }

        if (orderRepository.existsByDetailsOrderDetailAddOnsAddOnId(id)) {
            throw new IllegalArgumentException(
                    "No se puede eliminar este adicional porque está en pedidos. Puedes desactivarlo.");
        }

        repository.deleteById(id);
    }

    @Override
    @Transactional
    public AddOn toggleAddOnActive(Integer id) {

        AddOn addOn = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El adicional no existe."));

        addOn.setActive(!addOn.getActive());

        return repository.save(addOn);
    }
}