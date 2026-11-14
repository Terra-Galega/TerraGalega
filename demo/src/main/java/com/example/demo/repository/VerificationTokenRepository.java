package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entities.UserEntity;
import com.example.demo.entities.VerificationToken;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Integer> {

    // Busca el token por su hash (el token original nunca se guarda)
    Optional<VerificationToken> findByTokenHash(String tokenHash);

    // El token más reciente del usuario, para limitar los reenvíos
    Optional<VerificationToken> findFirstByUserOrderByCreatedAtDesc(UserEntity user);

    // Borra todos los tokens de un usuario
    void deleteByUser(UserEntity user);
}
