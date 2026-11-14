package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entities.Role;
import com.example.demo.entities.RoleEntity;
import com.example.demo.entities.UserEntity;
import com.example.demo.errors.ApiException;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;

// Maneja las credenciales de la tabla users: crear la cuenta con la contraseña
// cifrada, comprobar contraseñas, cambiarlas y actualizar el correo.
@Service
public class UserAccountService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Crea y guarda la fila en users con el correo, la contraseña cifrada y el rol.
    @Transactional
    public UserEntity createUser(String email, String rawPassword, Role role) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw ApiException.badRequest("La contraseña es obligatoria.");
        }
        if (rawPassword.length() > 70) {
            throw ApiException.badRequest("La contraseña es demasiado larga.");
        }
        // El correo debe ser único entre TODOS los tipos de usuario
        if (userRepository.existsByUsername(email)) {
            throw ApiException.conflict("Ya existe una cuenta registrada con ese correo electrónico.");
        }

        RoleEntity roleEntity = roleRepository.findByName(role.name())
                .orElseThrow(() -> ApiException.notFound("No existe el rol " + role.name() + "."));

        List<RoleEntity> roles = new ArrayList<>();
        roles.add(roleEntity);

        UserEntity user = UserEntity.builder()
                .username(email)
                .password(passwordEncoder.encode(rawPassword))
                .roles(roles)
                .build();

        return userRepository.save(user);
    }

    // Compara una contraseña escrita con la cifrada guardada
    public boolean passwordMatches(UserEntity user, String rawPassword) {
        return user != null && rawPassword != null
                && passwordEncoder.matches(rawPassword, user.getPassword());
    }

    // Cambia la contraseña validando primero la actual
    @Transactional
    public void changePassword(UserEntity user, String currentPassword, String newPassword) {
        if (user == null) {
            throw ApiException.notFound("La cuenta no tiene credenciales asociadas.");
        }
        PasswordRules.validateChange(user.getPassword(), currentPassword, newPassword, passwordEncoder);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // Si la persona cambia su correo, también cambia su username en users
    @Transactional
    public void updateUsername(UserEntity user, String newEmail) {
        if (user == null || newEmail == null || newEmail.equals(user.getUsername())) {
            return;
        }
        if (userRepository.existsByUsername(newEmail)) {
            throw ApiException.conflict("Ya existe otra cuenta con ese correo.");
        }
        user.setUsername(newEmail);
        userRepository.save(user);
    }
}