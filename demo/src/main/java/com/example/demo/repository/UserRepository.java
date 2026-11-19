package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entities.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> {

    // Busca el usuario por su correo (lo usa el login)
    Optional<UserEntity> findByUsername(String username);

    // Verifica si ya existe una cuenta con ese correo, sin importar el tipo
    boolean existsByUsername(String username);

    // Cuenta enlazada a una cuenta de Google (por el "sub" del token de Google)
    Optional<UserEntity> findByGoogleSubject(String googleSubject);

    Optional<UserEntity> findByMicrosoftIdentity(String microsoftIdentity);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<UserEntity> findFirstByUsername(String username);
}
