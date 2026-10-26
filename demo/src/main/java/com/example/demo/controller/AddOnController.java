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

@RestController
@RequestMapping("/addons")
@CrossOrigin(origins = "http://localhost:4200")
public class AddOnController {

    @Autowired
    private AddOnService addOnService;

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

    private AddOnOptionDTO toOptionDTO(AddOn addOn) {
        AddOnOptionDTO dto = new AddOnOptionDTO();
        dto.setId(addOn.getId());
        dto.setName(addOn.getName());
        dto.setPrice(addOn.getPrice());
        return dto;
    }

    private AddOn toEntity(AddOnDTO dto) {
        AddOn addOn = new AddOn();

        addOn.setId(dto.getId());
        addOn.setName(dto.getName());
        addOn.setDescription(dto.getDescription());
        addOn.setPrice(dto.getPrice());
        addOn.setActive(dto.getActive());

        Set<Category> categories = new HashSet<>();

        if (dto.getCategories() != null) {
            for (CategoryRefDTO categoryDTO : dto.getCategories()) {
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

    @Operation(summary = "Get all addons")
    @GetMapping
    public Collection<AddOnDTO> getAllAddOns() {
        return addOnService.getAllAddOns()
                .stream().map(this::toDTO).toList();
    }

    @Operation(summary = "Get all active addons")
    @GetMapping("/active")
    public Collection<AddOnDTO> getActiveAddOns() {
        return addOnService.getAllAddOnsActive()
                .stream().map(this::toDTO).toList();
    }

    @Operation(summary = "Get active addons of a category")
    @GetMapping("/active/category/{categoryId}")
    public Collection<AddOnOptionDTO> getActiveAddOnsByCategory(
            @PathVariable Integer categoryId) {
        return addOnService.getActiveAddOnsByCategory(categoryId)
                .stream().map(this::toOptionDTO).toList();
    }

    @Operation(summary = "Get addon by ID")
    @GetMapping("/{id}")
    public AddOnDTO getAddOnById(@PathVariable Integer id) {
        return toDTO(addOnService.getAddOnById(id));
    }

    @Operation(summary = "Add a new addon")
    @PostMapping
    public AddOnDTO addAddOn(@RequestBody AddOnDTO dto) {
        return toDTO(addOnService.addAddOn(toEntity(dto)));
    }

    @Operation(summary = "Update an addon")
    @PutMapping("/{id}")
    public AddOnDTO updateAddOn(
            @PathVariable Integer id,
            @RequestBody AddOnDTO dto) {
        return toDTO(addOnService.updateAddOn(id, toEntity(dto)));
    }

    @Operation(summary = "Delete an addon")
    @DeleteMapping("/{id}")
    public void deleteAddOn(@PathVariable Integer id) {
        addOnService.deleteAddOn(id);
    }

    @Operation(summary = "Toggle addon active status")
    @PutMapping("/{id}/toggle")
    public AddOnDTO toggleAddOn(@PathVariable Integer id) {
        return toDTO(addOnService.toggleAddOnActive(id));
    }
}