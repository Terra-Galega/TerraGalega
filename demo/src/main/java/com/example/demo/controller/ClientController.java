package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.service.ClientService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/*
 * CRUD completo de Clientes. Usa Bootstrap 5 (a diferencia del resto del
 * sitio, que usa Tailwind) para cumplir el requisito de usar ambos frameworks.
 */
@Controller
@RequestMapping("/clients")
public class ClientController {

    @Autowired
    private ClientService ClientService;

    // http://localhost:8090/clients -> listado (Read)
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("clients", ClientService.getAllClients());
        return "list";
    }

    // http://localhost:8090/clients/new -> formulario de creación (Create)
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("client", new Client());
        model.addAttribute("isNew", true);
        return "form";
    }

    // Guarda un cliente nuevo
    @PostMapping
    public String crear(@ModelAttribute Client Client) {
        ClientService.addClient(Client);
        return "redirect:/clients";
    }

    // http://localhost:8090/clients/{id}/edit -> formulario de edición (Update)
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model) {
        Client Client = ClientService.getClientById(id);
        if (Client == null) {
            return "redirect:/clients";
        }
        model.addAttribute("client", Client);
        model.addAttribute("isNew", false);
        return "form";
    }

    // Guarda los cambios de un cliente existente
    @PostMapping("/{id}")
    public String update(@PathVariable Integer id, @ModelAttribute Client Client) {
        ClientService.updateClient(id, Client);
        return "redirect:/clients";
    }

    // Elimina un cliente (Delete)
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id) {
        ClientService.deleteClient(id);
        return "redirect:/clients";
    }

    // http://localhost:8090/clients/{id} -> ver el detalle de un cliente (Read
    // individual)
    @GetMapping("/{id}")
    public String ver(@PathVariable Integer id, Model model) {
        Client Client = ClientService.getClientById(id);
        if (Client == null) {
            return "redirect:/clients";
        }
        model.addAttribute("client", Client);
        return "detail";
    }
}