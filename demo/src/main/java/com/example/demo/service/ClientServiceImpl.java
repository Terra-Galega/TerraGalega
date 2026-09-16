package com.example.demo.service;

import com.example.demo.entities.Client;
import com.example.demo.entities.UserRole;
import com.example.demo.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Service
public class ClientServiceImpl implements ClientService {

    @Autowired
    private ClientRepository repository;

    @Override
    @Transactional
    public Collection<Client> getAllClients() {
        return repository.findAll();
    }

    @Override
    @Transactional
    public Client getClientById(Integer id) {
        Client cliente = repository.findById(id).orElseThrow();

        if (cliente == null) {
            throw new RuntimeException("El cliente no existe");
        }

        return cliente;
    }

    @Override
    @Transactional
    public Client addClient(Client client) {
        // Gracias a cascade = CascadeType.ALL y @MapsId en Client.userRole,
        // guardar el Client también inserta su UserRole asociado y hace que
        // Client.id tome el mismo valor autogenerado que UserRole.id.
        return repository.save(client);
    }

    @Override
    @Transactional
    public Client updateClient(Integer id, Client client) {

        Client current = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El cliente no existe."));

        UserRole newData = client.getUserRole();

        Client emailClient = repository.findByUserRoleEmail(newData.getEmail());

        if (emailClient != null
                && !emailClient.getId().equals(id)) {

            throw new IllegalArgumentException(
                    "Ya existe otra cuenta con ese correo.");
        }

        // modificamos la entidad administrada (current) en vez de guardar el objeto
        // "client" recién construido, para no pelear con @MapsId al reasignar ids
        UserRole currentRole = current.getUserRole();
        currentRole.setName(newData.getName());
        currentRole.setLastName(newData.getLastName());
        currentRole.setEmail(newData.getEmail());

        if (newData.getPassword() != null
                && !newData.getPassword().isBlank()) {

            currentRole.setPassword(newData.getPassword());
        }

        current.setPhone(client.getPhone());

        return repository.save(current);
    }

    @Override
    @Transactional
    public void deleteClient(Integer id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public Client findByEmail(String email) {
        return repository.findByUserRoleEmail(email);
    }

}