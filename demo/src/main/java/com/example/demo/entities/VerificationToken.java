package com.example.demo.entities;

import java.time.LocalDateTime;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// Token de confirmación de correo. Solo se guarda su hash (SHA-256), nunca el
// token original: si alguien ve la base de datos no puede usarlos.
@Getter
@Setter
@ToString(exclude = { "tokenHash", "user" })
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "verification_tokens")
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // SHA-256 en hexadecimal (siempre 64 caracteres)
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    // OnDelete CASCADE: si se borra el usuario, sus tokens se borran con él
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // Login con Microsoft desactivado: la universidad no nos da permisos de Azure y con una
    // cuenta personal tampoco se pudo. La columna se deja porque la usa la consulta de
    // verificación de correo y quitarla cambiaría el esquema de la base de datos.
    // En el primer acceso con Microsoft todavía puede no existir un usuario local.
    @Column(unique = true, length = 73)
    private String microsoftIdentity;

}