package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.entities.Client;
import com.example.demo.entities.Role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Datos del cliente que se envían al front (perfil, editar cuenta y lista del admin).
// No incluye la contraseña, el usuario de seguridad ni las direcciones,
// que tienen su propio endpoint.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClientProfileDTO {

    private Integer id;
    private String name;
    private String lastName;
    private String email;
    private String phone;
    private Role role;

    // Suscripción premium: se calcula en el back con la hora actual
    private boolean subscribed;
    private LocalDateTime subscriptionEndsAt;
    private long daysLeft;

    public static ClientProfileDTO from(Client client) {
        return new ClientProfileDTO(client.getId(), client.getName(), client.getLastName(),
                client.getEmail(), client.getPhone(), client.getRole(),
                client.isSubscribed(), client.getSubscriptionEndsAt(), client.getDaysLeft());
    }
}