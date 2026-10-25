package com.example.demo.errors;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public String handlerProductNotFound(ProductNotFoundException e, Model model) {
        model.addAttribute("message", e.getMessage());
        return "error";
    }

    @ExceptionHandler(CategoryHasProductsException.class)
    public ResponseEntity<Map<String, Object>> handleCategoryHasProducts(CategoryHasProductsException e) {
        return ResponseEntity.badRequest().body(Map.of(
                "message", e.getMessage(),
                "products", e.getProducts()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException e) {
        e.printStackTrace();
        return ResponseEntity.status(500).body(Map.of("message", "Ha ocurrido un error inesperado."));
    }
}