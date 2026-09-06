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

}
