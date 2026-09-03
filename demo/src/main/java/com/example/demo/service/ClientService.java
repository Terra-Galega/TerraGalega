package com.example.demo.service;

import com.example.demo.entities.Client;

import java.util.Collection;

public interface ClientService {

    Collection<Client> getAllClients();

    Client getClientById(Integer id);

    Client addClient(Client client);

    Client updateClient(Integer id, Client client);

    void deleteClient(Integer id);

    Client login(String email, String password);
}