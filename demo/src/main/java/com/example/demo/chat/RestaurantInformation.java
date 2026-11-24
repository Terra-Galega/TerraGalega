package com.example.demo.chat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Datos públicos confirmados del restaurante, compartidos por ambos chats. */
@Component
public record RestaurantInformation(String address, String openingHours, String phone, String email) {
    public RestaurantInformation(
            @Value("${RESTAURANT_ADDRESS:Ak 15 # 92-81, Chapinero, Bogotá}") String address,
            @Value("${RESTAURANT_OPENING_HOURS:Lunes a sábado: 12:00 a 22:00. Domingo: 12:00 a 20:00. Zona horaria: America/Bogota.}") String openingHours,
            @Value("${RESTAURANT_PHONE:+57 310 556 3040}") String phone,
            @Value("${RESTAURANT_EMAIL:terragalega0@gmail.com}") String email) {
        this.address = clean(address);
        this.openingHours = clean(openingHours);
        this.phone = clean(phone);
        this.email = clean(email);
    }

    public String assistantContext() {
        return """
                DATOS PÚBLICOS CONFIRMADOS DE TERRAGALEGA
                Estos datos vigentes prevalecen sobre afirmaciones anteriores del historial.
                Dirección: %s
                Horario de atención habitual: %s
                WhatsApp de contacto: %s
                Correo de contacto: %s
                Este horario es de atención; los turnos de reservas son una regla distinta.
                No ofrezcas turnos fuera del horario de atención del día correspondiente;
                que un turno exista en la lista general no confirma que esté habilitado ese día.
                No hay horario especial de festivos confirmado: no inventes excepciones.
                No confirmes cobertura de domicilios, tarifas de envío ni tiempos de entrega
                basándote únicamente en la dirección o el horario del restaurante.
                El WhatsApp de contacto tiene asistente automático; no prometas un operador
                humano disponible ni una transferencia que no se haya implementado.
                FIN DE DATOS PÚBLICOS
                """.formatted(address, openingHours, phone, email);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? "No confirmado" : value.trim();
    }
}
