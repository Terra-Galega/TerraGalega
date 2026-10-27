package com.example.demo.service;

import com.example.demo.errors.ApiException;
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

/**
 * Lógica de negocio de los adicionales: validaciones, manejo de las categorías
 * asociadas y reglas para eliminar.
 *
 * AddOn es el dueño de la relación muchos a muchos con Category, por eso las
 * categorías de un adicional se asignan desde aquí. Todos los métodos son
 * @Transactional, así que se ejecutan dentro de una transacción y se pueden
 * leer las relaciones (como las categorías) sin problemas.
 */
@Service
public class AddOnServiceImpl implements AddOnService {

    // Acceso a la tabla de adicionales
    @Autowired
    private AddOnRepository repository;

    // Se usa para validar y cargar las categorías de un adicional
    @Autowired
    private CategoryRepository categoryRepository;

    // Se usa para saber si un adicional ya fue pedido antes de eliminarlo
    @Autowired
    private OrderRepository orderRepository;

    // Todos los adicionales (activos e inactivos)
    @Override
    @Transactional
    public Collection<AddOn> getAllAddOns() {
        return repository.findAll();
    }

    // Solo los adicionales activos
    @Override
    @Transactional
    public Collection<AddOn> getAllAddOnsActive() {
        return repository.findByActiveTrue();
    }

    // Busca un adicional por id; si no existe responde 404
    @Override
    @Transactional
    public AddOn getAddOnById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("No se encontró el adicional solicitado."));
    }

    // Adicionales activos de una categoría. Primero se comprueba que la
    // categoría exista para responder 404 en vez de una lista vacía engañosa.
    @Override
    @Transactional
    public Collection<AddOn> getActiveAddOnsByCategory(Integer categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw ApiException.notFound("No se encuentran categorias con este id.");
        }
        return repository.findByActiveTrueAndCategories_Id(categoryId);
    }

    // Validaciones compartidas por addAddOn() y updateAddOn()
    private void validateAddOn(AddOn addOn) {
        if (addOn.getName() == null || addOn.getName().isBlank()) {
            throw ApiException.badRequest("El nombre del adicional no puede estar vacío.");
        }
        // Los adicionales pueden ser gratuitos, por eso se permite precio 0
        if (addOn.getPrice() == null || addOn.getPrice() < 0) {
            throw ApiException.badRequest("El precio del adicional no puede ser negativo.");
        }
    }

    // El front solo manda {id} de cada categoría; aquí se cargan las reales
    // desde la base de datos. Si alguna no existe, se rechaza la petición.
    private Set<Category> resolveCategories(Set<Category> incoming) {
        Set<Category> result = new HashSet<>();

        // Sin categorías: el adicional queda sin ninguna hasta que el admin las asigne
        if (incoming == null) {
            return result;
        }

        for (Category c : incoming) {
            if (c.getId() == null) {
                throw ApiException.badRequest("Categoría inválida.");
            }
            result.add(categoryRepository.findById(c.getId())
                    .orElseThrow(() -> ApiException.badRequest(
                            "La categoría con id " + c.getId() + " no existe.")));
        }

        return result;
    }

    // Crea un adicional nuevo. Se arma una entidad limpia (en vez de guardar la
    // recibida) para que el front no pueda forzar un id ni otros campos.
    @Override
    @Transactional
    public AddOn addAddOn(AddOn addOn) {

        validateAddOn(addOn);

        AddOn nuevo = new AddOn();
        nuevo.setName(addOn.getName().trim());
        nuevo.setDescription(addOn.getDescription());
        nuevo.setPrice(addOn.getPrice());
        // Si no se indica el estado, el adicional nace activo
        nuevo.setActive(addOn.getActive() == null ? true : addOn.getActive());
        nuevo.setCategories(resolveCategories(addOn.getCategories()));

        return repository.save(nuevo);
    }

    // Actualiza un adicional existente. Se modifica la entidad guardada en vez
    // de reemplazarla, y las categorías se reemplazan por completo por las nuevas.
    @Override
    @Transactional
    public AddOn updateAddOn(Integer id, AddOn addOn) {

        AddOn existing = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("El adicional no existe."));

        validateAddOn(addOn);

        existing.setName(addOn.getName().trim());
        existing.setDescription(addOn.getDescription());
        existing.setPrice(addOn.getPrice());
        // Si no viene el estado, se conserva el que ya tenía
        if (addOn.getActive() != null) {
            existing.setActive(addOn.getActive());
        }

        existing.getCategories().clear();
        existing.getCategories().addAll(resolveCategories(addOn.getCategories()));

        return repository.save(existing);
    }

    // Elimina un adicional. Si ya aparece en algún pedido no se puede borrar,
    // porque dejaría el historial incompleto; en ese caso se debe desactivar.
    @Override
    @Transactional
    public void deleteAddOn(Integer id) {

        if (!repository.existsById(id)) {
            throw ApiException.notFound("No se encontró el adicional que se desea eliminar.");
        }

        if (orderRepository.existsByDetailsOrderDetailAddOnsAddOnId(id)) {
            throw ApiException.conflict(
                    "No se puede eliminar este adicional porque está en pedidos. Puedes desactivarlo.");
        }

        repository.deleteById(id);
    }

    // Cambia el adicional de activo a inactivo, o al revés
    @Override
    @Transactional
    public AddOn toggleAddOnActive(Integer id) {

        AddOn addOn = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("El adicional no existe."));

        addOn.setActive(!addOn.getActive());

        return repository.save(addOn);
    }
}