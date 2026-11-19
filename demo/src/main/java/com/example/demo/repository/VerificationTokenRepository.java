package com.example.demo.repository;

import java.util.Optional;
import java.time.LocalDateTime;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entities.UserEntity;
import com.example.demo.entities.VerificationToken;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Integer> {

    // Busca el token por su hash (el token original nunca se guarda)
    @Query("select t from VerificationToken t where t.tokenHash = ?1 and t.microsoftIdentity is null and t.user is not null")
    Optional<VerificationToken> findByTokenHash(String tokenHash);

    // El token más reciente del usuario, para limitar los reenvíos
    Optional<VerificationToken> findFirstByUserOrderByCreatedAtDesc(UserEntity user);

    // Borra todos los tokens de un usuario
    void deleteByUser(UserEntity user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<VerificationToken> findByMicrosoftIdentity(String identity);

    @Modifying
    @Query("delete from VerificationToken t where t.microsoftIdentity is not null and t.expiresAt < ?1")
    void removeExpiredMicrosoft(LocalDateTime now);
}
