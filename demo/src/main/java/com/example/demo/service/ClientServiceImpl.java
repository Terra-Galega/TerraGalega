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
        return repository.findById(id);
    }

    @Override
    public Client addClient(Client Client) {
        return repository.save(Client);
    }

    @Override
    public Client updateClient(Integer id, Client Client) {
        return repository.update(id, Client);
    }

    @Override
    public void deleteClient(Integer id) {
        repository.deleteById(id);
    }

    @Override
    public Client login(String email, String password) {
        Client Client = repository.findByEmail(email);
        if (Client != null && Client.getPassword().equals(password)) {
            return Client;
        }
        return null;
    }
}