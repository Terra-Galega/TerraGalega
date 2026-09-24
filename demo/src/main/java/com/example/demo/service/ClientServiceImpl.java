package com.example.demo.service;

import com.example.demo.entities.Client;
import com.example.demo.repository.AdminRepository;
import com.example.demo.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Service
public class ClientServiceImpl implements ClientService {

    @Autowired
    private ClientRepository repository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired 
    private OrderService orderService;

    @Override
    @Transactional
    public Collection<Client> getAllClients() {
        return repository.findAll();
    }

    @Override
    @Transactional
    public Client getClientById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el cliente solicitado."));
    }

    @Override
    @Transactional
    public Client addClient(Client Client) {
        return repository.save(Client);
    }

    @Override
    @Transactional
    public Client updateClient(Integer id, Client client) {

        Client actual = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El cliente no existe."));

        Client emailClient = repository.findByEmail(client.getEmail());

        if (emailClient != null
                && !emailClient.getId().equals(id)) {

            throw new IllegalArgumentException(
                    "Ya existe otra cuenta con ese correo.");
        }

        if (adminRepository.findByEmail(client.getEmail()) != null) {
            throw new IllegalArgumentException(
                    "Ya existe otra cuenta con ese correo.");
        }

        client.setId(actual.getId());

        // El formulario de edición no envía "role", así que lo preservamos
        // igual que se preserva la contraseña cuando viene vacía.
        client.setRole(actual.getRole());

        if (client.getPassword() == null
                || client.getPassword().isBlank()) {

            client.setPassword(actual.getPassword());
        }

        return repository.save(client);
    }

    @Override
    @Transactional
    public void deleteClient(Integer id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("No se encontró el cliente que se desea eliminar.");
        }
        // Antes de eliminar el cliente, limpiamos los id de los pedidos
        orderService.clearClientIdFromOrders(id);
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public Client findByEmail(String email) {
        return repository.findByEmail(email);
    }

}