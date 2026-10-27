package com.example.demo.controller;

import com.example.demo.dto.ChangePasswordRequestDTO;
import com.example.demo.entities.DeliveryPerson;
import com.example.demo.service.DeliveryPersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import com.example.demo.dto.DeliveryPersonOptionDTO;

import java.util.Collection;

@RestController
@RequestMapping("/delivery-persons")
@CrossOrigin(origins = "http://localhost:4200")
public class DeliveryPersonController {

    @Autowired
    private DeliveryPersonService deliveryPersonService;

    @Operation(summary = "Get all delivery persons for admin UI")
    @GetMapping
    public Collection<DeliveryPerson> getAllDeliveryPersons() {
        return deliveryPersonService.getAllDeliveryPersons();
    }

    @Operation(summary = "Get active delivery persons that are available, for the operator UI")
    @GetMapping("/available")
    public Collection<DeliveryPersonOptionDTO> getAvailableDeliveryPersons() {
        return deliveryPersonService.getAvailableDeliveryPersons().stream()
                .map(this::toOptionDTO)
                .toList();
    }

    @Operation(summary = "Get delivery person by ID")
    @GetMapping("/{id}")
    public DeliveryPerson getDeliveryPersonById(@PathVariable Integer id) {
        return deliveryPersonService.getDeliveryPersonById(id);
    }

    @Operation(summary = "Add a new delivery person from admin UI")
    @PostMapping
    public DeliveryPerson addDeliveryPerson(@RequestBody DeliveryPerson deliveryPerson) {
        return deliveryPersonService.addDeliveryPerson(deliveryPerson);
    }

    @Operation(summary = "Update a delivery person by ID from admin UI")
    @PutMapping("/{id}")
    public DeliveryPerson updateDeliveryPerson(
            @PathVariable Integer id,
            @RequestBody DeliveryPerson deliveryPerson) {

        return deliveryPersonService.updateDeliveryPerson(id, deliveryPerson);
    }

    @Operation(summary = "Activate or deactivate a delivery person (e.g. a day off)")
    @PutMapping("/{id}/toggle")
    public DeliveryPerson toggleDeliveryPerson(@PathVariable Integer id) {
        return deliveryPersonService.toggleDeliveryPersonActive(id);
    }

    @Operation(summary = "Delete a delivery person by ID from admin UI")
    @DeleteMapping("/{id}")
    public void deleteDeliveryPerson(@PathVariable Integer id) {
        deliveryPersonService.deleteDeliveryPerson(id);
    }

    @Operation(summary = "Change password (requires current password)")
    @PutMapping("/{id}/password")
    public void changePassword(@PathVariable Integer id,
            @RequestBody ChangePasswordRequestDTO request) {
        deliveryPersonService.changePassword(id, request.getCurrentPassword(), request.getNewPassword());
    }

    private DeliveryPersonOptionDTO toOptionDTO(DeliveryPerson deliveryPerson) {
        DeliveryPersonOptionDTO dto = new DeliveryPersonOptionDTO();
        dto.setId(deliveryPerson.getId());
        dto.setName(deliveryPerson.getName());
        dto.setLastName(deliveryPerson.getLastName());
        dto.setPhone(deliveryPerson.getPhone());
        return dto;
    }
}