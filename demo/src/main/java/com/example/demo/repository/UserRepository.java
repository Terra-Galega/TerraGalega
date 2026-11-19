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

    // LOGIN CON MICROSOFT DESACTIVADO (no se puede usar).
    // La universidad no nos da permisos en Azure (Microsoft Entra ID) para registrar la
    // aplicación y obtener el Client ID, así que no lo podemos usar. También se intentó
    // con una cuenta personal de Microsoft, pero tampoco lo permitió.
    // Se deja comentado por si en el futuro se consiguen los permisos.
    // Optional<UserEntity> findByMicrosoftIdentity(String microsoftIdentity);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<UserEntity> findFirstByUsername(String username);
}
