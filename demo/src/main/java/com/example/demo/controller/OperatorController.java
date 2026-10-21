package com.example.demo.controller;

import com.example.demo.entities.Operator;
import com.example.demo.service.OperatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

import java.util.Collection;

@RestController
@RequestMapping("/operators")
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
    @PutMapping("/{id}/toggle")
    public Operator toggleOperator(@PathVariable Integer id) {
        return operatorService.toggleOperatorActive(id);
    }
}