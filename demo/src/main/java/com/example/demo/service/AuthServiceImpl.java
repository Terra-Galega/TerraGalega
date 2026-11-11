package com.example.demo.service;

import com.example.demo.errors.ApiException;
import com.example.demo.entities.Admin;
import com.example.demo.entities.Client;
import com.example.demo.entities.Address;
import com.example.demo.entities.Operator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.demo.dto.AddressDTO;
import com.example.demo.dto.LoginResponseDTO;
import java.util.List;
import com.example.demo.repository.AdminRepository;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.OperatorRepository;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.entities.DeliveryPerson;
import com.example.demo.repository.DeliveryPersonRepository;

// Implementa la autenticación y las comprobaciones de existencia de cuentas.
@Service
public class AuthServiceImpl implements AuthService {

    // Servicios utilizados para buscar clientes, administradores y operadores por
    // correo.
    @Autowired
    private ClientService clientService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private OperatorService operatorService;

    // Repositorios utilizados para comprobar si las cuentas siguen existiendo.
    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private OperatorRepository operatorRepository;

    @Autowired
    private DeliveryPersonService deliveryPersonService;

    @Autowired
    private DeliveryPersonRepository deliveryPersonRepository;

    // Comprueba si la cuenta de una sesión abierta sigue existiendo según su tipo e
    // ID.
    @Override
    public boolean sessionExists(String type, Integer id) {
        return switch (type) {
            case "client" -> clientRepository.existsById(id);
            case "admin" -> adminRepository.existsById(id);
            case "operator" -> operatorRepository.existsById(id);
            case "delivery_person" -> deliveryPersonRepository.existsById(id);
            default -> false;
        };
    }

    // Mensaje que se devuelve cuando un operador intenta acceder con su cuenta
    // desactivada.
    private static final String INACTIVE_ACCOUNT_MESSAGE = "Tu cuenta está desactivada. Contacta al administrador.";

    // Busca al usuario por correo y valida sus credenciales según su tipo de
    // cuenta.
    @Override
    @Transactional
    public LoginResponseDTO login(String email, String password) {

        // Primero busca un cliente. Si las credenciales no coinciden, devuelve null
        // para que el controlador gestione el mensaje de error de autenticación.
        Client client = clientService.findByEmail(email);

        if (client != null) {
            return client.getPassword().equals(password)
                    ? new LoginResponseDTO(client.getId(), client.getName(),
                            client.getLastName(), client.getRole())
                    : null;
        }

        Admin admin = adminService.findByEmail(email);

        if (admin != null) {
            return admin.getPassword().equals(password)
                    ? new LoginResponseDTO(admin.getId(), admin.getName(),
                            admin.getLastName(), admin.getRole())
                    : null;
        }

        // Si no encuentra un administrador, busca un operador.
        // Los operadores solo pueden iniciar sesión si su cuenta está activa.
        Operator operator = operatorService.findByEmail(email);

        if (operator != null) {
            // Verifica que la contraseña del operador coincida con la proporcionada.
            if (!operator.getPassword().equals(password)) {
                return null;
            }

            // Impide el acceso cuando la cuenta del operador está desactivada.
            if (!Boolean.TRUE.equals(operator.getActive())) {
                throw ApiException.forbidden(INACTIVE_ACCOUNT_MESSAGE);
            }

            // El operador no tiene dirección, por eso se envía null en ese campo.
            return new LoginResponseDTO(operator.getId(), operator.getName(),
                    operator.getLastName(), operator.getRole());
        }

        // Domiciliarios (app móvil). Solo entran si el admin no los desactivó.
        DeliveryPerson deliveryPerson = deliveryPersonService.findByEmail(email);

        if (deliveryPerson != null) {
            if (!deliveryPerson.getPassword().equals(password)) {
                return null;
            }
            if (!Boolean.TRUE.equals(deliveryPerson.getActive())) {
                throw ApiException.forbidden(INACTIVE_ACCOUNT_MESSAGE);
            }
            return new LoginResponseDTO(deliveryPerson.getId(), deliveryPerson.getName(),
                    deliveryPerson.getLastName(), deliveryPerson.getRole());
        }
        // Devuelve null si no existe ninguna cuenta asociada al correo proporcionado.
        return null;
    }
}