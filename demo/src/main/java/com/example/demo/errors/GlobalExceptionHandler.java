package com.example.demo.errors;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.ui.Model;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)

    public String handlerProductNotFound(ProductNotFoundException e, Model model) {
        model.addAttribute("message", e.getMessage());
        return "error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handlerIllegalArgument(IllegalArgumentException e, Model model) {
        model.addAttribute("message", e.getMessage());
        return "error";
    }

    @ExceptionHandler(RuntimeException.class)
    public String handlerRuntime(RuntimeException e, Model model) {
        e.printStackTrace();
        model.addAttribute("message", "Ha ocurrido un error inesperado. Por favor intenta de nuevo.");
        return "error";
    }

}