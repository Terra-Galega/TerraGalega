package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

// EnableAsync: el correo de verificación se envía en segundo plano
@SpringBootApplication
@EnableAsync
public class DemoApplication {
    public static void main(String[] args) {
        // El restaurante está en Bogotá. Railway corre en UTC (5 h adelante), y las
        // reservas guardan fecha y hora "de pared" sin zona; sin esto, LocalDateTime.now()
        // del servidor no coincide con la hora que ve el cliente en la web o el móvil.
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Bogota"));
        SpringApplication.run(DemoApplication.class, args);
    }
}