package com.example.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.example.demo.entities.DeliveryPerson;
import com.example.demo.entities.Role;

/**
 * Pruebas de la consulta con @Query de DeliveryPersonRepository
 * findByAvailableTrueAndActiveTrue (domiciliarios que se pueden asignar).
 */
@DataJpaTest
class DeliveryPersonRepositoryQueriesTest {

    @Autowired
    private DeliveryPersonRepository deliveryPersonRepository;

    // Constructorrr, guarda un domiciliario. el número hace únicos email, cédula y teléfono
    private DeliveryPerson saveDeliveryPerson(String name, int number,
            boolean available, boolean active) {
        return deliveryPersonRepository.save(DeliveryPerson.builder()
                .name(name)
                .lastName("Prueba")
                .email(name.toLowerCase() + "@test.com")
                .password("Clave123*")
                .role(Role.DELIVERY_PERSON)
                .identification("100" + number)
                .phone("300000000" + number)
                .available(available)
                .active(active)
                .build());
    }

    // Antes de cada prueba: las 4 combinaciones posibles de available y active
    @BeforeEach
    void setUp() {
        saveDeliveryPerson("Asignable", 1, true, true);
        saveDeliveryPerson("Ocupado", 2, false, true);
        saveDeliveryPerson("Desactivado", 3, true, false);
        saveDeliveryPerson("OcupadoYDesactivado", 4, false, false);
    }

    @Test
    void DeliveryPersonRepository_FindByAvailableTrueAndActiveTrue_ReturnsOnlyAssignableOnes() {
        // Arrange: setUp() dejó 4 domiciliarios y solo uno cumple las dos condiciones

        // Act
        List<DeliveryPerson> assignable = deliveryPersonRepository.findByAvailableTrueAndActiveTrue();

        // Assert
        assertThat(assignable).hasSize(1);
        assertThat(assignable.get(0).getName()).isEqualTo("Asignable");
    }

    
}