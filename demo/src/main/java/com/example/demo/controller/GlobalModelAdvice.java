
package com.example.demo.controller;

import com.example.demo.entities.Client;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    /* Se ejecuta antes de cada handler y agrega el Client de la sesión
    (o null) a TODAS las vistas, con una clave que no choca con el
      @ModelAttribute Client de registro()/updateMyAccount(). */
    
    @ModelAttribute("loggedClient")
    public Client addLoggedClientToModel(HttpSession session) {
        return (Client) session.getAttribute(HomeController.SESSION_Client);
    }
}