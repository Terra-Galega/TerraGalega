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

import com.example.demo.dto.OrderDTO;
import com.example.demo.errors.ApiException;
import com.example.demo.movil.dto.DeliveryDTOs.ClientDeliveryDTO;
import com.example.demo.movil.dto.DeliveryDTOs.CompleteDeliveryRequestDTO;
import com.example.demo.movil.dto.DeliveryDTOs.DeliveryJobDTO;
import com.example.demo.movil.dto.DeliveryDTOs.LocationRequestDTO;
import com.example.demo.movil.service.DeliveryFlowService;
import com.example.demo.security.AuthenticatedUser;

import io.swagger.v3.oas.annotations.Operation;

import com.example.demo.movil.dto.DeliveryDTOs.OperatorDeliveryDTO;
import com.example.demo.movil.dto.DeliveryDTOs.StartDeliveryRequestDTO;

// Seguimiento de domicilios.
// SecurityConfig controla los roles y aquí comprobamos que el ID
// recibido corresponde a la persona identificada por el JWT.
@RestController
@RequestMapping("/delivery")
public class DeliveryTrackingController {

    @Autowired
    private DeliveryFlowService flow;

    @Autowired
    private AuthenticatedUser authenticatedUser;

    // ================= CLIENTE =================

    @Operation(summary = "Client: own active deliveries with PIN and courier location")
    @GetMapping("/client/{clientId}/active")
    public List<ClientDeliveryDTO> activeForClient(
            @PathVariable Integer clientId) {

        requireCurrentClient(clientId);

        return flow.activeForClient(clientId);
    }

    @Operation(summary = "Client: tracking of one of their own deliveries")
    @GetMapping("/client/{clientId}/orders/{orderId}")
    public ClientDeliveryDTO forClient(
            @PathVariable Integer clientId,
            @PathVariable Integer orderId) {

        requireCurrentClient(clientId);

        // El servicio también comprueba que el pedido pertenezca al cliente.
        return flow.forClient(clientId, orderId);
    }

    // ================= REPARTIDOR =================

    @Operation(summary = "Delivery person: own assigned active deliveries")
    @GetMapping("/person/{deliveryPersonId}/active")
    public List<DeliveryJobDTO> activeForDeliveryPerson(
            @PathVariable Integer deliveryPersonId) {

        requireCurrentDeliveryPerson(deliveryPersonId);

        return flow.activeForDeliveryPerson(deliveryPersonId);
    }

    @Operation(summary = "Delivery person: report location for an assigned order")
    @PutMapping("/person/{deliveryPersonId}/orders/{orderId}/location")
    public DeliveryJobDTO updateLocation(
            @PathVariable Integer deliveryPersonId,
            @PathVariable Integer orderId,
            @RequestBody LocationRequestDTO body) {

        requireCurrentDeliveryPerson(deliveryPersonId);

        // El servicio comprueba la asignación del pedido,
        // su estado y la validez de las coordenadas.
        return flow.updateLocation(
                deliveryPersonId,
                orderId,
                body.latitude(),
                body.longitude());
    }

    @Operation(summary = "Delivery person: complete an assigned order using the client's PIN")
    @PostMapping("/person/{deliveryPersonId}/orders/{orderId}/complete")
    public OrderDTO complete(
            @PathVariable Integer deliveryPersonId,
            @PathVariable Integer orderId,
            @RequestBody CompleteDeliveryRequestDTO body) {

        requireCurrentDeliveryPerson(deliveryPersonId);

        // El servicio comprueba la asignación, el estado y el PIN.
        return flow.complete(deliveryPersonId, orderId, body.pin());
    }

    // ================= VALIDACIONES DE IDENTIDAD =================

    // Tener rol CLIENT no permite consultar los domicilios de otro cliente.
    private void requireCurrentClient(Integer clientId) {
        Integer authenticatedId = authenticatedUser.client().getId();

        if (!authenticatedId.equals(clientId)) {
            throw ApiException.forbidden(
                    "No puedes consultar los domicilios de otro cliente.");
        }
    }

    // Tener rol DELIVERY_PERSON no permite actuar como otro repartidor.
    private void requireCurrentDeliveryPerson(Integer deliveryPersonId) {
        Integer authenticatedId = authenticatedUser.deliveryPerson().getId();

        if (!authenticatedId.equals(deliveryPersonId)) {
            throw ApiException.forbidden(
                    "No puedes consultar ni modificar los domicilios de otro repartidor.");
        }
    }

    @Operation(summary = "Delivery person: start an assigned delivery")
    @PostMapping("/person/{deliveryPersonId}/orders/{orderId}/start")
    public DeliveryJobDTO start(
            @PathVariable Integer deliveryPersonId,
            @PathVariable Integer orderId,
            @RequestBody StartDeliveryRequestDTO body) {

        requireCurrentDeliveryPerson(deliveryPersonId);

        return flow.start(deliveryPersonId, orderId, body.route());
    }

    @Operation(summary = "Operator: deliveries whose routes have started")
    @GetMapping("/operator/active")
    public List<OperatorDeliveryDTO> activeForOperator() {
        authenticatedUser.operator();

        return flow.activeForOperator();
    }
}