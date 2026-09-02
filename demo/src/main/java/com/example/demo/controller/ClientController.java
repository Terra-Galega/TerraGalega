package com.example.demo.controller;

import com.example.demo.entities.Client;
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

    // http://localhost:8090/clients/me/edit
    @GetMapping("/me/edit")
    public String editMyAccountForm(Model model, HttpSession session) {
        Client actual = (Client) session.getAttribute(HomeController.SESSION_Client);
        if (actual == null) {
            return "redirect:/login";
        }
        model.addAttribute("client", actual);
        return "account-edit";
    }

    // Guarda los cambios del perfil
    @PostMapping("/me/edit")
    public String updateMyAccount(@ModelAttribute Client formClient, Model model, HttpSession session) {

        Client actual = (Client) session.getAttribute(HomeController.SESSION_Client);
        if (actual == null) {
            return "redirect:/login";
        }

        boolean emailEnUso = clientService.getAllClients().stream()
                .anyMatch(
                        c -> !c.getId().equals(actual.getId()) && c.getEmail().equalsIgnoreCase(formClient.getEmail()));
        if (emailEnUso) {
            model.addAttribute("editError", "Ya existe otra cuenta con ese correo.");
            model.addAttribute("client", actual);
            return "account-edit";
        }

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