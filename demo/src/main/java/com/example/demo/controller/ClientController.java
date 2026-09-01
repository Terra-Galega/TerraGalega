package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.service.ClientService;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/*
 * Controlador de Clientes: acá vive todo lo que un Client hace sobre SU
 * PROPIA cuenta (editar sus datos, eliminarla). Requiere sesión activa
 * (ver HomeController.SESSION_Client). El login/registro sigue en
 * HomeController porque ahí es donde se maneja la sesión de entrada al sitio.
 */
@Controller
@RequestMapping("/clients")
public class ClientController {

    @Autowired
    private ClientService clientService;

    // http://localhost:8090/clients/me/edit -> formulario para editar MI perfil
    @GetMapping("/me/edit")
    public String editMyAccountForm(Model model, HttpSession session) {
        Client actual = (Client) session.getAttribute(HomeController.SESSION_Client);
        if (actual == null) {
            return "redirect:/login";
        }
        model.addAttribute("client", actual);
        return "account-edit";
    }

    // Guarda los cambios de MI perfil
    @PostMapping("/me/edit")
    public String updateMyAccount(@ModelAttribute Client formClient, Model model, HttpSession session) {

        Client actual = (Client) session.getAttribute(HomeController.SESSION_Client);
        if (actual == null) {
            return "redirect:/login";
        }

        // Evita que otro Client ya esté usando ese correo
        boolean emailEnUso = clientService.getAllClients().stream()
                .anyMatch(c -> !c.getId().equals(actual.getId()) && c.getEmail().equalsIgnoreCase(formClient.getEmail()));
        if (emailEnUso) {
            model.addAttribute("editError", "Ya existe otra cuenta con ese correo.");
            model.addAttribute("client", actual);
            return "account-edit";
        }

        // "id", "admin" y "active" NO se toman del formulario (ahí llegan en null
        // porque el form no los manda): se fuerzan desde la sesión para que el
        // propio Client no pueda auto-asignarse el rol de admin ni reactivarse.
        formClient.setId(actual.getId());
        formClient.setAdmin(actual.getAdmin());
        formClient.setActive(actual.getActive());
        if (formClient.getPassword() == null || formClient.getPassword().isBlank()) {
            formClient.setPassword(actual.getPassword());
        }

        Client guardado = clientService.updateClient(actual.getId(), formClient);
        session.setAttribute(HomeController.SESSION_Client, guardado);
        return "redirect:/account";
    }

    // El propio Client elimina SU cuenta (no lo hace el admin)
    @PostMapping("/me/delete")
    public String deleteMyAccount(HttpSession session) {
        Client actual = (Client) session.getAttribute(HomeController.SESSION_Client);
        if (actual == null) {
            return "redirect:/login";
        }
        clientService.deleteClient(actual.getId());
        session.invalidate();
        return "redirect:/home";
    }
}