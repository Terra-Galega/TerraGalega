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

// Indica que esta clase contiene la lógica de negocio para gestionar clientes.
@Service
public class ClientServiceImpl implements ClientService {

    // Repositorio encargado de consultar y modificar los clientes.
    @Autowired
    private ClientRepository repository;

    // Repositorio utilizado para comprobar si un correo pertenece a un administrador.
    @Autowired
    private AdminRepository adminRepository;

    // Repositorio utilizado para comprobar si un correo pertenece a un operador.
    @Autowired
    private OperatorRepository operatorRepository;

    // Repositorio utilizado para comprobar si un correo pertenece a un repartidor.
    @Autowired
    private DeliveryPersonRepository deliveryPersonRepository;

    // Servicio encargado de gestionar los pedidos asociados a los clientes.
    @Autowired
    private OrderService orderService;

    // Obtiene todos los clientes registrados en la base de datos.
    @Override
    @Transactional
    public Collection<Client> getAllClients() {
        return repository.findAll();
    }

    // Busca un cliente por su ID; si no existe, genera un error 404.
    @Override
    @Transactional
    public Client getClientById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("No se encontró el cliente solicitado."));
    }

    // Registra un cliente después de validar su correo y comprobar que no existan duplicados.
    @Override
    @Transactional
    public Client addClient(Client client) {

        // Verifica que el correo cumpla con las reglas establecidas.
        EmailRules.validate(client.getEmail());

        // El correo no puede estar usado por ningún tipo de usuario.
        if (repository.findByEmail(client.getEmail()) != null
                || adminRepository.findByEmail(client.getEmail()) != null
                || operatorRepository.findByEmail(client.getEmail()) != null
                || deliveryPersonRepository.findByEmail(client.getEmail()) != null) {
            throw ApiException.conflict(
                    "Ya existe una cuenta registrada con ese correo electrónico.");
        }

        // Comprueba que el número de teléfono no esté registrado por otro cliente.
        if (repository.findByPhone(client.getPhone()) != null) {
            throw ApiException.conflict(
                    "Ya existe una cuenta registrada con ese número de teléfono.");
        }

        // Guarda el cliente en la base de datos y devuelve el registro creado.
        return repository.save(client);
    }

    // Actualiza la información de un cliente existente.
    @Override
    @Transactional
    public Client updateClient(Integer id, Client client) {

        // Busca el cliente que se va a actualizar; si no existe, genera un error 404.
        Client actual = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("El cliente no existe."));

        // Valida el formato del correo recibido.
        EmailRules.validate(client.getEmail());

        // Consulta si el correo o el teléfono ya están registrados en otro cliente.
        Client emailClient = repository.findByEmail(client.getEmail());
        Client phoneClient = repository.findByPhone(client.getPhone());

        // Impide usar un teléfono que pertenezca a otro cliente.
        if (phoneClient != null && !phoneClient.getId().equals(id)) {
            throw ApiException.conflict(
                    "Ya existe otra cuenta con ese número de teléfono.");
        }

        // Impide usar un correo que pertenezca a otro cliente.
        if (emailClient != null
                && !emailClient.getId().equals(id)) {

            throw ApiException.conflict(
                    "Ya existe otra cuenta con ese correo.");
        }

        // Comprueba que el correo no esté registrado por un administrador.
        if (adminRepository.findByEmail(client.getEmail()) != null) {
            throw ApiException.conflict(
                    "Ya existe otra cuenta con ese correo.");
        }

        // Conserva el identificador del cliente que se está actualizando.
        client.setId(actual.getId());

        // El formulario de edición no envía "role", así que lo preservamos
        // igual que se preserva la contraseña cuando viene vacía.
        client.setRole(actual.getRole());

        // La contraseña solo se cambia por /clients/{id}/password (exige la actual).
        client.setPassword(actual.getPassword());

        // Guarda los datos actualizados del cliente.
        return repository.save(client);
    }

    // Elimina un cliente y gestiona previamente las relaciones con sus pedidos.
    @Override
    @Transactional
    public void deleteClient(Integer id) {
        // Verifica que el cliente exista antes de intentar eliminarlo.
        if (!repository.existsById(id)) {
            throw ApiException.notFound("No se encontró el cliente que se desea eliminar.");
        }

        // Valida que no haya pedidos en curso y cancela los pendientes.
        orderService.cancelPendingOrdersForClientDeletion(id);

        // Antes de eliminar el cliente, limpiamos los id de los pedidos.
        orderService.clearClientIdFromOrders(id);

        // Elimina el cliente de la base de datos.
        repository.deleteById(id);
    }

    // Busca un cliente mediante su correo electrónico.
    @Override
    @Transactional
    public Client findByEmail(String email) {
        return repository.findByEmail(email);
    }

    // Cambia la contraseña después de comprobar que el cliente existe y validar las credenciales.
    @Override
    @Transactional
    public void changePassword(Integer id, String currentPassword, String newPassword) {
        // Busca al cliente que solicita el cambio de contraseña.
        Client client = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("No se encontró el cliente solicitado."));

        // Verifica la contraseña actual y las condiciones para establecer la nueva.
        PasswordRules.validateChange(client.getPassword(), currentPassword, newPassword);

        // Asigna la nueva contraseña y guarda los cambios.
        client.setPassword(newPassword);
        repository.save(client);
    }

}