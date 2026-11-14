package com.example.demo.controller;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.ChangePasswordRequestDTO;
import com.example.demo.dto.DeliveryPersonDTO;
import com.example.demo.dto.DeliveryPersonOptionDTO;
import com.example.demo.dto.DeliveryPersonRequestDTO;
import com.example.demo.entities.DeliveryPerson;
import com.example.demo.service.DeliveryPersonService;

import io.swagger.v3.oas.annotations.Operation;

// API REST de domiciliarios. Ruta base: /delivery-persons.
@RestController
@RequestMapping("/delivery-persons")
@CrossOrigin(origins = "http://localhost:4200")
public class DeliveryPersonController {

    @Autowired
    private DeliveryPersonService deliveryPersonService;

    // =========================
    // OPERADOR
    // =========================

    // Domiciliarios que el operador puede asignar a un pedido (con datos reducidos)
    @Operation(summary = "Get active delivery persons that are available (operator)")
    @GetMapping("/available")
    public Collection<DeliveryPersonOptionDTO> getAvailableDeliveryPersons() {
        return deliveryPersonService.getAvailableDeliveryPersons().stream()
                .map(this::toOptionDTO)
                .toList();
    }

    // =========================
    // ADMIN
    // =========================

    @Operation(summary = "Get all delivery persons")
    @GetMapping
    public Collection<DeliveryPersonDTO> getAllDeliveryPersons() {
        return deliveryPersonService.getAllDeliveryPersons().stream().map(DeliveryPersonDTO::from).toList();
    }

    @Operation(summary = "Get delivery person by ID")
    @GetMapping("/{id}")
    public DeliveryPersonDTO getDeliveryPersonById(@PathVariable Integer id) {
        return DeliveryPersonDTO.from(deliveryPersonService.getDeliveryPersonById(id));
    }

    @Operation(summary = "Add a new delivery person")
    @PostMapping
    public DeliveryPersonDTO addDeliveryPerson(@RequestBody DeliveryPersonRequestDTO request) {
        return DeliveryPersonDTO.from(deliveryPersonService.addDeliveryPerson(request.toEntity()));
    }

    @Operation(summary = "Update a delivery person by ID")
    @PutMapping("/{id}")
    public DeliveryPersonDTO updateDeliveryPerson(@PathVariable Integer id,
            @RequestBody DeliveryPersonRequestDTO request) {
        return DeliveryPersonDTO.from(deliveryPersonService.updateDeliveryPerson(id, request.toEntity()));
    }

    @Operation(summary = "Activate or deactivate a delivery person (e.g. a day off)")
    @PutMapping("/{id}/toggle")
    public DeliveryPersonDTO toggleDeliveryPerson(@PathVariable Integer id) {
        return DeliveryPersonDTO.from(deliveryPersonService.toggleDeliveryPersonActive(id));
    }

    @Operation(summary = "Delete a delivery person by ID")
    @DeleteMapping("/{id}")
    public void deleteDeliveryPerson(@PathVariable Integer id) {
        deliveryPersonService.deleteDeliveryPerson(id);
    }

    @Operation(summary = "Change password (requires current password)")
    @PutMapping("/{id}/password")
    public void changePassword(@PathVariable Integer id, @RequestBody ChangePasswordRequestDTO request) {
        deliveryPersonService.changePassword(id, request.getCurrentPassword(), request.getNewPassword());
    }

    // Convierte la entidad al DTO reducido que ve el operador
    private DeliveryPersonOptionDTO toOptionDTO(DeliveryPerson deliveryPerson) {
        DeliveryPersonOptionDTO dto = new DeliveryPersonOptionDTO();
        dto.setId(deliveryPerson.getId());
        dto.setName(deliveryPerson.getName());
        dto.setLastName(deliveryPerson.getLastName());
        dto.setPhone(deliveryPerson.getPhone());
        return dto;
    }
}
