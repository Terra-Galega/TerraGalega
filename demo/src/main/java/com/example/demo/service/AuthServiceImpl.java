package com.example.demo.service;

import com.example.demo.errors.ApiException;
import com.example.demo.entities.Admin;
import com.example.demo.entities.Client;
import com.example.demo.entities.Operator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.demo.dto.LoginResponseDTO;
import com.example.demo.repository.AdminRepository;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.OperatorRepository;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private ClientService clientService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private OperatorService operatorService;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private OperatorRepository operatorRepository;

    // especificamos si la cuenta de una sesión abierta sigue existiendo
    @Override
    public boolean sessionExists(String type, Integer id) {
        return switch (type) {
            case "client" -> clientRepository.existsById(id);
            case "admin" -> adminRepository.existsById(id);
            case "operator" -> operatorRepository.existsById(id);
            default -> false;
        };
    }

    // Mensaje cuando el admin desactivó la cuenta
    private static final String INACTIVE_ACCOUNT_MESSAGE = "Tu cuenta está desactivada. Contacta al administrador.";

    @Override
    public LoginResponseDTO login(String email, String password) {

        // Devuelve null (no lanza excepción) cuando el correo no existe o la
        // contraseña no coincide, AuthController lo convierte en el mensaje
        // "Correo o contraseña incorrectos."
        Client client = clientService.findByEmail(email);

        if (client != null) {
            return client.getPassword().equals(password)
                    ? new LoginResponseDTO(client.getId(), client.getName(),
                            client.getLastName(), client.getAddress(), client.getRole())
                    : null;
        }

        Admin admin = adminService.findByEmail(email);

        if (admin != null) {
            // El admin no tiene dirección
            return admin.getPassword().equals(password)
                    ? new LoginResponseDTO(admin.getId(), admin.getName(),
                            admin.getLastName(), null, admin.getRole())
                    : null;
        }

        // Los operadores solo pueden iniciar sesión si el admin los tiene activos
        Operator operator = operatorService.findByEmail(email);

        if (operator != null) {
            if (!operator.getPassword().equals(password)) {
                return null;
            }

            if (!Boolean.TRUE.equals(operator.getActive())) {
                throw ApiException.forbidden(INACTIVE_ACCOUNT_MESSAGE);
            }

            // El operador no tiene dirección
            return new LoginResponseDTO(operator.getId(), operator.getName(),
                    operator.getLastName(), null, operator.getRole());
        }

        return null;
    }
}