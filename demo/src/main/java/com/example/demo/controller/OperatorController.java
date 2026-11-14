package com.example.demo.controller;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.ChangePasswordRequestDTO;
import com.example.demo.dto.OperatorDTO;
import com.example.demo.dto.OperatorRequestDTO;
import com.example.demo.security.AuthenticatedUser;
import com.example.demo.service.OperatorService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/operators")
@CrossOrigin(origins = "http://localhost:4200")
public class OperatorController {

    @Autowired
    private OperatorService operatorService;

    @Autowired
    private AuthenticatedUser authenticatedUser;

    // =========================
    // OPERADOR CON SESIÓN
    // =========================

    // Lo usa el panel del operador para mostrar sus datos
    @Operation(summary = "Get the logged-in operator")
    @GetMapping("/me")
    public OperatorDTO getMe() {
        return OperatorDTO.from(authenticatedUser.operator());
    }

    // =========================
    // ADMIN
    // =========================

    @Operation(summary = "Get all operators")
    @GetMapping
    public Collection<OperatorDTO> getAllOperators() {
        return operatorService.getAllOperators().stream().map(OperatorDTO::from).toList();
    }

    @Operation(summary = "Get operator by ID")
    @GetMapping("/{id}")
    public OperatorDTO getOperatorById(@PathVariable Integer id) {
        return OperatorDTO.from(operatorService.getOperatorById(id));
    }

    @Operation(summary = "Add a new operator")
    @PostMapping
    public OperatorDTO addOperator(@RequestBody OperatorRequestDTO request) {
        return OperatorDTO.from(operatorService.addOperator(request.toEntity()));
    }

    // La contraseña no se actualiza por aquí: se cambia únicamente en /{id}/password
    @Operation(summary = "Update an operator by ID")
    @PutMapping("/{id}")
    public OperatorDTO updateOperator(@PathVariable Integer id, @RequestBody OperatorRequestDTO request) {
        return OperatorDTO.from(operatorService.updateOperator(id, request.toEntity()));
    }

    @Operation(summary = "Delete an operator by ID")
    @DeleteMapping("/{id}")
    public void deleteOperator(@PathVariable Integer id) {
        operatorService.deleteOperator(id);
    }

    // Alterna activo/inactivo sin recibir cuerpo; el servidor decide el nuevo estado
    @Operation(summary = "Toggle operator active status")
    @PutMapping("/{id}/toggle")
    public OperatorDTO toggleOperator(@PathVariable Integer id) {
        return OperatorDTO.from(operatorService.toggleOperatorActive(id));
    }

    // El servicio verifica la contraseña actual antes de aplicar la nueva
    @Operation(summary = "Change password (requires current password)")
    @PutMapping("/{id}/password")
    public void changePassword(@PathVariable Integer id, @RequestBody ChangePasswordRequestDTO request) {
        operatorService.changePassword(id, request.getCurrentPassword(), request.getNewPassword());
    }
}
