package com.example.demo.repository;

import com.example.demo.entities.Client;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Repository
public class ClientRepository {

    private final Map<Integer, Client> ClientMap = new HashMap<>();

    public ClientRepository() {
        ClientMap.put(1, new Client(1, "Juan", "García", "cliente@terra.com", "cliente123",
                "+57 300 000 0000", "Calle 93 #15-47, Bogotá", true, false));
        ClientMap.put(2, new Client(2, "Marta", "Souto", "marta.souto@terra.com", "marta123",
                "+57 310 555 1122", "Carrera 11 #82-30, Bogotá", true, false));
        ClientMap.put(3, new Client(3, "Diego", "Pardo", "diego.pardo@terra.com", "diego123",
                "+57 320 444 9988", "Av. 19 #104-20, Bogotá", true, false));
        ClientMap.put(4, new Client(4, "Admin", "Terra Galega", "admin@terra.com", "admin123",
                "+57 300 000 0001", "Oficina Terra Galega", true, true));
    }

    // Devuelve todos los Clients
    public Collection<Client> findAll() {
        return ClientMap.values();
    }

    // Devuelve un Client por su id
    public Client findById(Integer id) {
        return ClientMap.get(id);
    }

    // Devuelve un Client por su email (usado para el login)
    public Client findByEmail(String email) {
        if (email == null) {
            return null;
        }
        return ClientMap.values().stream()
                .filter(c -> c.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElse(null);
    }

    // Calcula el siguiente id disponible (el mayor id actual mas 1)
    private Integer nextId() {
        return ClientMap.keySet().stream().max(Integer::compareTo).orElse(0) + 1;
    }

    // Crea un Client nuevo, le asigna id y lo marca activo por defecto
    public Client save(Client Client) {
        Client.setId(nextId());
        if (Client.getActive() == null) {
            Client.setActive(true);
        }
        ClientMap.put(Client.getId(), Client);
        return Client;
    }

    // Actualiza los datos de un Client existente sin perder su id
    public Client update(Integer id, Client Client) {
        Client existing = ClientMap.get(id);
        if (existing == null) {
            return null;
        }
        Client.setId(id);
        if (Client.getActive() == null) {
            Client.setActive(existing.getActive());
        }
        ClientMap.put(id, Client);
        return Client;
    }

    // Elimina un Client por su id
    public void deleteById(Integer id) {
        ClientMap.remove(id);
    }
}