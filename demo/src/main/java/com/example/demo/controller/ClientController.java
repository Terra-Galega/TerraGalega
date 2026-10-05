package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clients")
@CrossOrigin(origins = "http://localhost:4200")
public class ClientController {

    @Autowired
    private ClientService clientService;

    // http://localhost:8080/clients/{id}/edit
    @GetMapping("/{id}/edit")
    public void editMyAccountForm(@PathVariable Integer id) {
        clientService.getClientById(id);
    }

    // Guarda los cambios del perfil
    @PutMapping("/{id}")
    public void updateMyAccount(@PathVariable Integer id, @ModelAttribute Client formClient) {
        clientService.updateClient(id, formClient);
    }

    @DeleteMapping("/{id}/delete")
    public void deleteMyAccount(@PathVariable Integer id) {
        clientService.deleteClient(id);
    }
}