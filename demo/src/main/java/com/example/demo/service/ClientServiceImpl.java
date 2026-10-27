package com.example.demo.service;

import com.example.demo.errors.ApiException;
import com.example.demo.entities.Client;
import com.example.demo.repository.AdminRepository;
import com.example.demo.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.repository.DeliveryPersonRepository;
import com.example.demo.repository.OperatorRepository;

import java.util.Collection;

@Service
public class ClientServiceImpl implements ClientService {

    @Autowired
    private ClientRepository repository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private OperatorRepository operatorRepository;

    @Autowired
    private DeliveryPersonRepository deliveryPersonRepository;

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
                .orElseThrow(() -> ApiException.notFound("No se encontró el cliente solicitado."));
    }

    @Override
    @Transactional
    public Client addClient(Client client) {

        EmailRules.validate(client.getEmail());

        // El correo no puede estar usado por ningún tipo de usuario
        if (repository.findByEmail(client.getEmail()) != null
                || adminRepository.findByEmail(client.getEmail()) != null
                || operatorRepository.findByEmail(client.getEmail()) != null
                || deliveryPersonRepository.findByEmail(client.getEmail()) != null) {
            throw ApiException.conflict(
                    "Ya existe una cuenta registrada con ese correo electrónico.");
        }

        if (repository.findByPhone(client.getPhone()) != null) {
            throw ApiException.conflict(
                    "Ya existe una cuenta registrada con ese número de teléfono.");
        }

        return repository.save(client);
    }

    @Override
    @Transactional
    public Client updateClient(Integer id, Client client) {

        Client actual = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("El cliente no existe."));

        EmailRules.validate(client.getEmail());

        Client emailClient = repository.findByEmail(client.getEmail());
        Client phoneClient = repository.findByPhone(client.getPhone());

        if (phoneClient != null && !phoneClient.getId().equals(id)) {
            throw ApiException.conflict(
                    "Ya existe otra cuenta con ese número de teléfono.");
        }

        if (emailClient != null
                && !emailClient.getId().equals(id)) {

            throw ApiException.conflict(
                    "Ya existe otra cuenta con ese correo.");
        }

        if (adminRepository.findByEmail(client.getEmail()) != null) {
            throw ApiException.conflict(
                    "Ya existe otra cuenta con ese correo.");
        }

        client.setId(actual.getId());

        // El formulario de edición no envía "role", así que lo preservamos
        // igual que se preserva la contraseña cuando viene vacía.
        client.setRole(actual.getRole());

        // La contraseña solo se cambia por /clients/{id}/password (exige la actual)
        client.setPassword(actual.getPassword());

        return repository.save(client);
    }

    @Override
    @Transactional
    public void deleteClient(Integer id) {
        if (!repository.existsById(id)) {
            throw ApiException.notFound("No se encontró el cliente que se desea eliminar.");
        }

        // Valida que no haya pedidos en curso y cancela los pendientes
        orderService.cancelPendingOrdersForClientDeletion(id);

        // Antes de eliminar el cliente, limpiamos los id de los pedidos
        orderService.clearClientIdFromOrders(id);
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public Client findByEmail(String email) {
        return repository.findByEmail(email);
    }

    @Override
    @Transactional
    public void changePassword(Integer id, String currentPassword, String newPassword) {
        Client client = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("No se encontró el cliente solicitado."));

        PasswordRules.validateChange(client.getPassword(), currentPassword, newPassword);

        client.setPassword(newPassword);
        repository.save(client);
    }

}