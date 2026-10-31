package com.example.demo.repository;

import java.util.List;

import com.example.demo.entities.DeliveryPerson;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Repositorio de domiciliarios. Hereda de JpaRepository las operaciones CRUD
 * básicas; los métodos de abajo son consultas derivadas del nombre del método.
 * Los de búsqueda única devuelven null si no encuentran nada.
 */
@Repository
public interface DeliveryPersonRepository extends JpaRepository<DeliveryPerson, Integer> {

    // Busca un deliveryPerson por email
    // (se usa en el login y para validar que el correo no esté repetido)
    DeliveryPerson findByEmail(String email);

    // Domiciliarios disponibles (no llevan pedido) y activos (no desactivados por
    // el admin)
    @Query("""
            select d from DeliveryPerson d
            where d.available = true
              and d.active = true
            """)
    List<DeliveryPerson> findByAvailableTrueAndActiveTrue();

    // Busqueda por Id del deliveryPerson, evitar registro con misma identificación
    DeliveryPerson findByIdentification(String identification);

    // Busqueda por telefono del deliveryPerso, para evitar duplicados
    DeliveryPerson findByPhone(String phone);
}