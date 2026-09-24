package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.entities.Role;
import com.example.demo.service.ClientService;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/clients")
public class ClientController {

    @Autowired
    private ClientService clientService;

    public static final String SESSION_Client = "ClientLogueado";

    // http://localhost:8080/clients/{id}/edit
    @GetMapping("/{id}/edit")
    public String editMyAccountForm(
            @PathVariable Integer id,
            Model model) {

        try {
            Client actual = clientService.getClientById(id);

            model.addAttribute("client", actual);
            return "account-edit";

        } catch (IllegalArgumentException e) {
            return "redirect:/login";
        }
    }

    // Guarda los cambios del perfil
    @PostMapping("/{id}")
    public String updateMyAccount(
            @PathVariable Integer id,
            @ModelAttribute Client formClient,
            Model model) {

        // El formulario de edición no manda "role"; lo fijamos explícitamente
        // (ClientServiceImpl igual lo preserva desde el registro actual por
        // si este valor llegara nulo).
        formClient.setRole(Role.CLIENT);

        try {
            clientService.updateClient(id, formClient);

            return "redirect:/account/" + id;

        } catch (IllegalArgumentException e) {

            model.addAttribute("editError", e.getMessage());

            Client actual = clientService.getClientById(id);
            model.addAttribute("client", actual);

            return "account-edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteMyAccount(
            @PathVariable Integer id,
            HttpSession session) {

        try {
            clientService.deleteClient(id);
            session.invalidate();
            return "redirect:/home";

        } catch (IllegalArgumentException e) {
            return "redirect:/login";
        }

    }
}