package com.example.demo.service;

import com.example.demo.entities.Admin;
import com.example.demo.entities.Client;
import com.example.demo.entities.DeliveryPerson;
import com.example.demo.entities.Operator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private ClientService clientService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private OperatorService operatorService;

    @Autowired
    private DeliveryPersonService deliveryPersonService;

    // Mensaje cuando el admin desactivó la cuenta
    private static final String INACTIVE_ACCOUNT_MESSAGE = "Tu cuenta está desactivada. Contacta al administrador.";

    @Override
    public Object login(String email, String password) {

        // Devuelve null (no lanza excepción) cuando el correo no existe o la
        // contraseña no coincide: AuthController lo convierte en el mensaje
        // "Correo o contraseña incorrectos."
        Client client = clientService.findByEmail(email);

        if (client != null) {
            return client.getPassword().equals(password) ? client : null;
        }

        Admin admin = adminService.findByEmail(email);

        if (admin != null) {
            return admin.getPassword().equals(password) ? admin : null;
        }

        // Operadores y domiciliarios solo pueden iniciar sesión si el admin
        // los tiene activos. Se revisa después de la contraseña para no
        // revelar a quien la adivina qué cuentas existen o están inactivas.
        Operator operator = operatorService.findByEmail(email);

        if (operator != null) {
            if (!operator.getPassword().equals(password)) {
                return null;
            }

            if (!Boolean.TRUE.equals(operator.getActive())) {
                throw new IllegalArgumentException(INACTIVE_ACCOUNT_MESSAGE);
            }

            return operator;
        }

        DeliveryPerson deliveryPerson = deliveryPersonService.findByEmail(email);

        if (deliveryPerson != null) {
            if (!deliveryPerson.getPassword().equals(password)) {
                return null;
            }

            if (!Boolean.TRUE.equals(deliveryPerson.getActive())) {
                throw new IllegalArgumentException(INACTIVE_ACCOUNT_MESSAGE);
            }

            return deliveryPerson;
        }

        return null;
    }
}