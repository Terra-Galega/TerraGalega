package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entities.PasswordResetToken;
import com.example.demo.entities.UserEntity;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {

    // Busca el token por su hash (el token original nunca se guarda)
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    // El token más reciente del usuario, para limitar las solicitudes
    Optional<PasswordResetToken> findFirstByUserOrderByCreatedAtDesc(UserEntity user);

    // Borra todos los tokens de recuperación de un usuario
    void deleteByUser(UserEntity user);
}
