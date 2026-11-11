package com.example.demo.movil.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.movil.dto.DeliveryDTOs.ClientDeliveryDTO;
import com.example.demo.movil.dto.DeliveryDTOs.CompleteDeliveryRequestDTO;
import com.example.demo.movil.dto.DeliveryDTOs.DeliveryJobDTO;
import com.example.demo.movil.dto.DeliveryDTOs.LocationRequestDTO;
import com.example.demo.movil.service.DeliveryFlowService;
import com.example.demo.dto.OrderDTO;

import io.swagger.v3.oas.annotations.Operation;

// API REST del seguimiento de domicilios (app móvil). Ruta base: /delivery.
@RestController
@RequestMapping("/delivery")
public class DeliveryTrackingController {

    @Autowired
    private DeliveryFlowService flow;

    // ---------- CLIENTE ----------

    @Operation(summary = "Client: active deliveries with PIN and courier location")
    @GetMapping("/client/{clientId}/active")
    public List<ClientDeliveryDTO> activeForClient(@PathVariable Integer clientId) {
        return flow.activeForClient(clientId);
    }

    @Operation(summary = "Client: one delivery (poll this to move the courier on the map)")
    @GetMapping("/client/{clientId}/orders/{orderId}")
    public ClientDeliveryDTO forClient(@PathVariable Integer clientId, @PathVariable Integer orderId) {
        return flow.forClient(clientId, orderId);
    }

    // ---------- REPARTIDOR ----------

    @Operation(summary = "Delivery person: orders being delivered")
    @GetMapping("/person/{deliveryPersonId}/active")
    public List<DeliveryJobDTO> activeForDeliveryPerson(@PathVariable Integer deliveryPersonId) {
        return flow.activeForDeliveryPerson(deliveryPersonId);
    }

    @Operation(summary = "Delivery person: report current GPS position")
    @PutMapping("/person/{deliveryPersonId}/orders/{orderId}/location")
    public DeliveryJobDTO updateLocation(@PathVariable Integer deliveryPersonId,
            @PathVariable Integer orderId, @RequestBody LocationRequestDTO body) {
        return flow.updateLocation(deliveryPersonId, orderId, body.latitude(), body.longitude());
    }

    @Operation(summary = "Delivery person: finish the order with the client's PIN")
    @PostMapping("/person/{deliveryPersonId}/orders/{orderId}/complete")
    public OrderDTO complete(@PathVariable Integer deliveryPersonId,
            @PathVariable Integer orderId, @RequestBody CompleteDeliveryRequestDTO body) {
        return flow.complete(deliveryPersonId, orderId, body.pin());
    }
}