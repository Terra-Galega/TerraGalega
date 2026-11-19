package com.example.demo.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.demo.entities.Client;
import com.example.demo.entities.DeliveryPerson;
import com.example.demo.entities.Operator;
import com.example.demo.errors.ApiException;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.DeliveryPersonRepository;
import com.example.demo.repository.OperatorRepository;

// Indica quién está haciendo la petición, a partir del usuario que el filtro JWT
// dejó en el SecurityContextHolder. Gracias a esto los endpoints "/me" no
// necesitan recibir el id del usuario en la URL.
@Component
public class AuthenticatedUser {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OperatorRepository operatorRepository;

    @Autowired
    private DeliveryPersonRepository deliveryPersonRepository;

    @Autowired
    private com.example.demo.repository.WaiterRepository waiterRepository;

    // Correo del usuario que viene en el token. Sin sesión responde 401.
    public String email() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw ApiException.unauthorized("Debes iniciar sesión para realizar esta acción.");
        }
        return authentication.getName();
    }

    // Cliente con sesión iniciada. Si el token es de otro tipo de usuario responde 403.
    public Client client() {
        Client client = clientRepository.findByEmail(email());
        if (client == null) {
            throw ApiException.forbidden("Esta acción es solo para clientes.");
        }
        return client;
    }

    // Operador con sesión iniciada. Si el token es de otro tipo de usuario responde 403.
    public Operator operator() {
        Operator operator = operatorRepository.findByEmail(email());
        if (operator == null) {
            throw ApiException.forbidden("Esta acción es solo para operadores.");
        }
        return operator;
    }

    // Mesero con sesión iniciada (pedidos de mesa en la app móvil)
    public com.example.demo.entities.Waiter waiter() {
        com.example.demo.entities.Waiter waiter = waiterRepository.findByEmail(email());
        if (waiter == null) {
            throw ApiException.forbidden("Esta acción es solo para meseros.");
        }
        return waiter;
    }

    // Domiciliario con sesión iniciada (para la app móvil)
    public DeliveryPerson deliveryPerson() {
        DeliveryPerson deliveryPerson = deliveryPersonRepository.findByEmail(email());
        if (deliveryPerson == null) {
            throw ApiException.forbidden("Esta acción es solo para domiciliarios.");
        }
        return deliveryPerson;
    }
}
