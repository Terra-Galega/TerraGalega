package com.example.demo.controller;

import com.example.demo.dto.ChangePasswordRequestDTO;
import com.example.demo.entities.Operator;
import com.example.demo.service.OperatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

import java.util.Collection;

@RestController
@RequestMapping("/operators")
// Solo permite peticiones desde el front en desarrollo (Angular, puerto 4200).
@CrossOrigin(origins = "http://localhost:4200")
public class OperatorController {

    @Autowired
    private OperatorService operatorService;

    @Operation(summary = "Get all operators")
    @GetMapping
    public Collection<Operator> getAllOperators() {
        return operatorService.getAllOperators();
    }

    @Operation(summary = "Get operator by ID")
    @GetMapping("/{id}")
    public Operator getOperatorById(@PathVariable Integer id) {
        return operatorService.getOperatorById(id);
    }

    @Operation(summary = "Add a new operator for registration from operator UI")
    @PostMapping
    public Operator addOperator(@RequestBody Operator operator) {
        return operatorService.addOperator(operator);
    }

    // La contraseña no se actualiza por aquí: se cambia únicamente en
    // /{id}/password.
    @Operation(summary = "Update an operator by ID from operator UI")
    @PutMapping("/{id}")
    public Operator updateOperator(
            @PathVariable Integer id,
            @RequestBody Operator operator) {

        return operatorService.updateOperator(id, operator);
    }

    @Operation(summary = "Delete an operator by ID from operator UI")
    @DeleteMapping("/{id}")
    public void deleteOperator(@PathVariable Integer id) {
        operatorService.deleteOperator(id);
    }

    @Operation(summary = "Toggle operator active status")
    // Alterna activo/inactivo sin recibir cuerpo; el servidor decide el nuevo
    // estado.
    @PutMapping("/{id}/toggle")
    public Operator toggleOperator(@PathVariable Integer id) {
        return operatorService.toggleOperatorActive(id);
    }

    // El servicio verifica la contraseña actual antes de aplicar la nueva.
    @Operation(summary = "Change password (requires current password)")
    @PutMapping("/{id}/password")
    public void changePassword(@PathVariable Integer id,
            @RequestBody ChangePasswordRequestDTO request) {
        operatorService.changePassword(id, request.getCurrentPassword(), request.getNewPassword());
    }
}