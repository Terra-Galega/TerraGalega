package com.example.demo.service;

import com.example.demo.entities.Client;
import com.example.demo.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class ClientServiceImpl implements ClientService {

    @Autowired
    private ClientRepository repository;

    @Override
    public Collection<Client> getAllClients() {
        return repository.findAll();
    }

    @Override
    public Client getClientById(Integer id) {
        Client cliente = repository.findById(id);

        if (cliente == null) {
            throw new RuntimeException("El cliente no existe");
        }

        return cliente;
    }

    @Override
    public Client addClient(Client Client) {
        return repository.save(Client);
    }

    @Override
    public Client updateClient(Integer id, Client client) {

        Client actual = repository.findById(id);

        if (actual == null) {
            throw new IllegalArgumentException("El cliente no existe.");
        }

        Client clienteConEmail = repository.findByEmail(client.getEmail());

        if (clienteConEmail != null
                && !clienteConEmail.getId().equals(id)) {

            throw new IllegalArgumentException(
                    "Ya existe otra cuenta con ese correo."
            );
        }

        client.setId(actual.getId());
        client.setAdmin(actual.getAdmin());
        client.setActive(actual.getActive());

        if (client.getPassword() == null
                || client.getPassword().isBlank()) {

            client.setPassword(actual.getPassword());
        }

        return repository.update(id, client);
    }

    @Override
    public void deleteClient(Integer id) {
        repository.deleteById(id);
    }

    @Override
    public Client login(String email, String password) {

        Client client = repository.findByEmail(email);

        if (client == null) {
            throw new IllegalArgumentException("El correo no está registrado.");
        }

        if (!client.getPassword().equals(password)) {
            throw new IllegalArgumentException("La contraseña es incorrecta.");
        }

        return client;
    }
}