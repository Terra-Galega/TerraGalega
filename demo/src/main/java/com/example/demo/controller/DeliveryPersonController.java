package com.example.demo.controller;

import com.example.demo.entities.DeliveryPerson;
import com.example.demo.service.DeliveryPersonService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/delivery-persons")
@CrossOrigin(origins = "http://localhost:4200")
public class DeliveryPersonController {

    @Autowired
    private DeliveryPersonService deliveryPersonService;

    @Operation(summary = "Get active delivery persons that are available, for the operator UI")
    @GetMapping("/available")
    public Collection<DeliveryPerson> getAvailableDeliveryPersons() {
        return deliveryPersonService.getAvailableDeliveryPersons();
    }
}