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

    // Compara la contraseña escrita con la cifrada de la tabla users
    @Autowired
    private UserAccountService userAccountService;

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

        Client client = clientService.findByEmail(email);

        if (client != null) {
            return userAccountService.passwordMatches(client.getUser(), password)
                    ? new LoginResponseDTO(client.getId(), client.getName(),
                            client.getLastName(), client.getRole())
                    : null;
        }

        Admin admin = adminService.findByEmail(email);

        if (admin != null) {
            return userAccountService.passwordMatches(admin.getUser(), password)
                    ? new LoginResponseDTO(admin.getId(), admin.getName(),
                            admin.getLastName(), admin.getRole())
                    : null;
        }

        // Los operadores solo pueden iniciar sesión si su cuenta está activa.
        Operator operator = operatorService.findByEmail(email);

        if (operator != null) {
            if (!userAccountService.passwordMatches(operator.getUser(), password)) {
                return null;
            }
            if (!Boolean.TRUE.equals(operator.getActive())) {
                throw ApiException.forbidden(INACTIVE_ACCOUNT_MESSAGE);
            }
            return new LoginResponseDTO(operator.getId(), operator.getName(),
                    operator.getLastName(), operator.getRole());
        }

        // Domiciliarios: solo entran si el admin no los desactivó.
        DeliveryPerson deliveryPerson = deliveryPersonService.findByEmail(email);

        if (deliveryPerson != null) {
            if (!userAccountService.passwordMatches(deliveryPerson.getUser(), password)) {
                return null;
            }
            if (!Boolean.TRUE.equals(deliveryPerson.getActive())) {
                throw ApiException.forbidden(INACTIVE_ACCOUNT_MESSAGE);
            }
            return new LoginResponseDTO(deliveryPerson.getId(), deliveryPerson.getName(),
                    deliveryPerson.getLastName(), deliveryPerson.getRole());
        }

        return null;
    }
}