package com.example.demo.controller;

import com.example.demo.entities.Admin;
import com.example.demo.entities.Client;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    /*
     Se ejecuta antes de cada handler y agrega el Client o Admin logueado
    (o null) a TODAS las vistas, con una clave que no choca con el
     @ModelAttribute Client de registro()/updateMyAccount()
     */

    @ModelAttribute("loggedClient")
    public Object addLoggedClientToModel(HttpSession session) {
        Object logged = session.getAttribute(AuthController.SESSION_Client);
        return (logged instanceof Client || logged instanceof Admin) ? logged : null;
    }


    @ModelAttribute("isLoggedAdmin")
    public boolean isLoggedAdmin(HttpSession session) {
        return session.getAttribute(AuthController.SESSION_Client) instanceof Admin;
    }
}