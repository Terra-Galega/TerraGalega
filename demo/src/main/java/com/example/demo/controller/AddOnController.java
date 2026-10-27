package com.example.demo.controller;

import com.example.demo.dto.AddOnDTO;
import com.example.demo.dto.CategoryDTO;
import com.example.demo.entities.AddOn;
import com.example.demo.entities.Category;
import com.example.demo.service.AddOnService;
import com.example.demo.dto.AddOnOptionDTO;
import com.example.demo.dto.CategoryRefDTO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controlador REST de los adicionales (addons) de los platos.
 * Expone el CRUD completo, el cambio de estado activo/inactivo y la consulta
 * de adicionales activos por categoría que usa el cliente al elegir su plato.
 *
 * Nunca entrega ni recibe la entidad AddOn directamente: convierte siempre
 * desde/hacia DTOs para controlar qué datos viajan entre el back y el front.
 */
@RestController
@RequestMapping("/addons")
// Permite las peticiones del front de Angular que corre en localhost:4200
@CrossOrigin(origins = "http://localhost:4200")
public class AddOnController {

    // La lógica de negocio (validaciones, reglas) vive en el servicio
    @Autowired
    private AddOnService addOnService;

    // Entidad -> DTO completo (vista del admin): incluye descripción, estado y
    // las categorías en las que aparece el adicional, cada una como referencia
    // corta (id y nombre) para no exponer la entidad Category entera.
    private AddOnDTO toDTO(AddOn addOn) {
        AddOnDTO dto = new AddOnDTO();
        dto.setId(addOn.getId());
        dto.setName(addOn.getName());
        dto.setDescription(addOn.getDescription());
        dto.setPrice(addOn.getPrice());
        dto.setActive(addOn.getActive());
        dto.setCategories(addOn.getCategories().stream()
                .map(category -> {
                    CategoryRefDTO ref = new CategoryRefDTO();
                    ref.setId(category.getId());
                    ref.setName(category.getName());
                    return ref;
                })
                .collect(Collectors.toSet()));
        return dto;
    }

    // Entidad -> DTO reducido (vista del cliente): solo lo necesario para
    // elegir un adicional (id, nombre y precio), sin descripción, estado ni
    // categorías.
    private AddOnOptionDTO toOptionDTO(AddOn addOn) {
        AddOnOptionDTO dto = new AddOnOptionDTO();
        dto.setId(addOn.getId());
        dto.setName(addOn.getName());
        dto.setPrice(addOn.getPrice());
        return dto;
    }

    // DTO -> entidad, para crear o actualizar. Del front solo llega el id de
    // cada categoría, así que se arman Category "vacías" con ese id; el servicio
    // se encarga después de buscar las categorías reales en la base de datos.
    private AddOn toEntity(AddOnDTO dto) {
        AddOn addOn = new AddOn();

        addOn.setId(dto.getId());
        addOn.setName(dto.getName());
        addOn.setDescription(dto.getDescription());
        addOn.setPrice(dto.getPrice());
        addOn.setActive(dto.getActive());

        Set<Category> categories = new HashSet<>();

        // Las categorías son opcionales: un adicional puede quedar sin ninguna
        if (dto.getCategories() != null) {
            for (CategoryRefDTO categoryDTO : dto.getCategories()) {
                // Se ignoran las referencias sin id porque no se pueden resolver
                if (categoryDTO.getId() != null) {
                    Category category = new Category();
                    category.setId(categoryDTO.getId());
                    categories.add(category);
                }
            }
        }

        addOn.setCategories(categories);
        return addOn;
    }

    // GET /addons -> todos los adicionales (activos e inactivos), para el admin
    @Operation(summary = "Get all addons")
    @GetMapping
    public Collection<AddOnDTO> getAllAddOns() {
        return addOnService.getAllAddOns()
                .stream().map(this::toDTO).toList();
    }

    // GET /addons/active -> solo los adicionales activos
    @Operation(summary = "Get all active addons")
    @GetMapping("/active")
    public Collection<AddOnDTO> getActiveAddOns() {
        return addOnService.getAllAddOnsActive()
                .stream().map(this::toDTO).toList();
    }

    // GET /addons/active/category/{categoryId} -> adicionales activos que
    // pertenecen a una categoría. Lo usa el cliente en el detalle de un plato:
    // devuelve el DTO reducido porque no necesita más datos.
    @Operation(summary = "Get active addons of a category")
    @GetMapping("/active/category/{categoryId}")
    public Collection<AddOnOptionDTO> getActiveAddOnsByCategory(
            @PathVariable Integer categoryId) {
        return addOnService.getActiveAddOnsByCategory(categoryId)
                .stream().map(this::toOptionDTO).toList();
    }

    // GET /addons/{id} -> un adicional por su id
    @Operation(summary = "Get addon by ID")
    @GetMapping("/{id}")
    public AddOnDTO getAddOnById(@PathVariable Integer id) {
        return toDTO(addOnService.getAddOnById(id));
    }

    // POST /addons -> crea un adicional nuevo (el servicio valida los datos)
    @Operation(summary = "Add a new addon")
    @PostMapping
    public AddOnDTO addAddOn(@RequestBody AddOnDTO dto) {
        return toDTO(addOnService.addAddOn(toEntity(dto)));
    }

    // PUT /addons/{id} -> actualiza un adicional existente, incluidas las
    // categorías en las que aparece
    @Operation(summary = "Update an addon")
    @PutMapping("/{id}")
    public AddOnDTO updateAddOn(
            @PathVariable Integer id,
            @RequestBody AddOnDTO dto) {
        return toDTO(addOnService.updateAddOn(id, toEntity(dto)));
    }

    // DELETE /addons/{id} -> elimina un adicional. El servicio lo impide si ya
    // fue usado en algún pedido (en ese caso se debe desactivar).
    @Operation(summary = "Delete an addon")
    @DeleteMapping("/{id}")
    public void deleteAddOn(@PathVariable Integer id) {
        addOnService.deleteAddOn(id);
    }

    // PUT /addons/{id}/toggle -> alterna el adicional entre activo e inactivo
    @Operation(summary = "Toggle addon active status")
    @PutMapping("/{id}/toggle")
    public AddOnDTO toggleAddOn(@PathVariable Integer id) {
        return toDTO(addOnService.toggleAddOnActive(id));
    }
}