package com.example.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.example.demo.entities.DeliveryPerson;
import com.example.demo.entities.Role;

/**
 * Pruebas del CRUD básico de DeliveryPersonRepository.
 */
@DataJpaTest
class DeliveryPersonRepositoryTest {

    @Autowired
    private DeliveryPersonRepository deliveryPersonRepository;

    // Domiciliarios guardados antes de cada prueba
    private DeliveryPerson carlos;
    private DeliveryPerson maria;

    // Constructor para reusarse en varias pruebas
    // Construye un domiciliario válido; email, identificación y teléfono
    private DeliveryPerson buildDeliveryPerson(String name, String email,
            String identification, String phone) {
        return DeliveryPerson.builder()
                .name(name)
                .lastName("Prueba")
                .email(email)
                .password("Clave123*")
                .role(Role.DELIVERY_PERSON)
                .identification(identification)
                .phone(phone)
                .available(true)
                .active(true)
                .build();
    }

    // Se ejecuta antes de cada prueba: deja exactamente 2 domiciliarios guardados
    @BeforeEach
    void setUp() {
        carlos = deliveryPersonRepository.save(
                buildDeliveryPerson("Carlos", "carlos@test.com", "1001", "3001111111"));
        maria = deliveryPersonRepository.save(
                buildDeliveryPerson("Maria", "maria@test.com", "1002", "3002222222"));
    }
    //Create
    @Test
    void DeliveryPersonRepository_Save_ReturnsSavedDeliveryPerson() {
        // Arrange: un domiciliario nuevo 
        DeliveryPerson nuevo = buildDeliveryPerson(
                "Pedro", "pedro@test.com", "1003", "3003333333");

        // Act
        DeliveryPerson guardado = deliveryPersonRepository.save(nuevo);

        // Assert: se generó el id y se conservaron los datos
        assertThat(guardado).isNotNull();
        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getName()).isEqualTo("Pedro");
        assertThat(guardado.getEmail()).isEqualTo("pedro@test.com");
    }
    //REad
    @Test
    void DeliveryPersonRepository_FindById_ReturnsDeliveryPerson() {
        // Arrange: usamos el id de Carlos, que ya está guardado en setUp()
        Integer id = carlos.getId();

        // Act
        Optional<DeliveryPerson> encontrado = deliveryPersonRepository.findById(id);

        // Assert
        assertThat(encontrado).isNotNull();
        assertThat(encontrado.get().getName()).isEqualTo("Carlos");
    }
    //Read
    @Test
    void DeliveryPersonRepository_FindAll_ReturnsAllDeliveryPersons() {
        // Arrange: setUp() ya guardó 2 domiciliarios

        // Act
        List<DeliveryPerson> todos = deliveryPersonRepository.findAll();

        // Assert
        assertThat(todos).isNotNull();
        assertThat(todos).hasSize(2);
        assertThat(todos).extracting(DeliveryPerson::getName)
                .containsExactlyInAnyOrder("Carlos", "Maria");
    }
    //Update
    @Test
    void DeliveryPersonRepository_Save_UpdatesExistingDeliveryPerson() {

        // Arrange: se modifican el teléfono y la disponibilidad de María
        Integer id = maria.getId();

        // Act: save sobre un id existente actualiza en lugar de insertar
        DeliveryPerson deliveryPerson = deliveryPersonRepository.findById(id).get();
        deliveryPerson.setPhone("3009999999");
        deliveryPerson.setAvailable(false);
        DeliveryPerson updatedDeliveryPerson = deliveryPersonRepository.save(deliveryPerson);

        // Assert: cambiaron los datos y no se creó un registro nuevo
        assertThat(updatedDeliveryPerson.getPhone()).isEqualTo("3009999999");
        assertThat(updatedDeliveryPerson.getAvailable()).isFalse();
        assertThat(deliveryPersonRepository.findAll()).hasSize(2);
    }
    //Delete
    @Test
    void DeliveryPersonRepository_DeleteById_RemovesDeliveryPerson() {
        // Arrange: se elimina a Carlos
        Integer id = carlos.getId();

        // Act
        deliveryPersonRepository.deleteById(id);

        // Assert: ya no existe y queda solo María
        assertThat(deliveryPersonRepository.findById(id)).isEmpty();
        assertThat(deliveryPersonRepository.findAll()).hasSize(1);
    }
}