package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.service.ClientService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/clients")
public class ClientController {

    @Autowired
    private ClientService clientService;

    // http://localhost:8090/clients/{id}/edit
    @GetMapping("/{id}/edit")
    public String editMyAccountForm(
            @PathVariable Integer id,
            Model model) {

        try {
            Client actual = clientService.getClientById(id);

            model.addAttribute("client", actual);
            return "account-edit";

        } catch (RuntimeException e) {
            return "redirect:/login";
        }
    }


    // Guarda los cambios del perfil
    @PostMapping("/{id}")
    public String updateMyAccount(
            @PathVariable Integer id,
            @ModelAttribute Client formClient,
            Model model) {

        try {
            clientService.updateClient(id, formClient);

            return "redirect:/account/" + id;

        } catch (RuntimeException e) {

            model.addAttribute("editError", e.getMessage());

            Client actual = clientService.getClientById(id);
            model.addAttribute("client", actual);

            return "account-edit";
        }
    }


    @PostMapping("/{id}/delete")
    public String deleteMyAccount(
            @PathVariable Integer id) {

        try {
            clientService.deleteClient(id);

            return "redirect:/home";

        } catch (RuntimeException e) {
            return "redirect:/login";
        }
    }
}