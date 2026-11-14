package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.AuthResponseDTO;
import com.example.demo.dto.LoginResponseDTO;
import com.example.demo.entities.Admin;
import com.example.demo.entities.Client;
import com.example.demo.entities.DeliveryPerson;
import com.example.demo.entities.Operator;
import com.example.demo.entities.UserEntity;
import com.example.demo.errors.ApiException;
import com.example.demo.repository.AdminRepository;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.DeliveryPersonRepository;
import com.example.demo.repository.OperatorRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtGenerator;

// Inicio de sesión con el AuthenticationManager de Spring Security y JWT.
// La validación del correo, la contraseña y el estado de la cuenta la hace
// Spring; este servicio solo genera el token y arma la respuesta.
@Service
public class AuthServiceImpl implements AuthService {

    private static final String INACTIVE_ACCOUNT_MESSAGE = "Tu cuenta está desactivada. Contacta al administrador.";
    private static final String EMAIL_NOT_VERIFIED_MESSAGE = "Debes confirmar tu correo electrónico antes de iniciar sesión. Revisa tu bandeja de entrada.";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtGenerator jwtGenerator;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private OperatorRepository operatorRepository;

    @Autowired
    private DeliveryPersonRepository deliveryPersonRepository;

    @Override
    @Transactional
    public AuthResponseDTO login(String email, String password) {
        Authentication authentication;
        try {
            // Busca el usuario con CustomUserDetailsService y compara la contraseña con BCrypt
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password));
        } catch (DisabledException e) {
            throw ApiException.forbidden(INACTIVE_ACCOUNT_MESSAGE);
        } catch (AuthenticationException e) {
            // Mismo mensaje si el correo no existe o la contraseña es incorrecta,
            // para no revelar qué correos están registrados
            throw ApiException.unauthorized("Correo o contraseña incorrectos.");
        }

        // La contraseña es correcta, pero la cuenta no entra hasta confirmar el correo.
        // Se revisa después de la contraseña para no revelar qué correos existen.
        UserEntity user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> ApiException.unauthorized("Correo o contraseña incorrectos."));
        if (!user.isEmailVerified()) {
            throw ApiException.emailNotVerified(EMAIL_NOT_VERIFIED_MESSAGE);
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtGenerator.generateToken(authentication);

        LoginResponseDTO profile = currentUser(email);
        return new AuthResponseDTO(token, profile.getId(), profile.getName(),
                profile.getLastName(), profile.getRole());
    }

    // Cada tipo de usuario tiene su propia tabla, así que se busca en cada una
    @Override
    @Transactional
    public LoginResponseDTO currentUser(String email) {
        Client client = clientRepository.findByEmail(email);
        if (client != null) {
            return new LoginResponseDTO(client.getId(), client.getName(), client.getLastName(), client.getRole());
        }

        Admin admin = adminRepository.findByEmail(email);
        if (admin != null) {
            return new LoginResponseDTO(admin.getId(), admin.getName(), admin.getLastName(), admin.getRole());
        }

        Operator operator = operatorRepository.findByEmail(email);
        if (operator != null) {
            return new LoginResponseDTO(operator.getId(), operator.getName(), operator.getLastName(),
                    operator.getRole());
        }

        DeliveryPerson deliveryPerson = deliveryPersonRepository.findByEmail(email);
        if (deliveryPerson != null) {
            return new LoginResponseDTO(deliveryPerson.getId(), deliveryPerson.getName(),
                    deliveryPerson.getLastName(), deliveryPerson.getRole());
        }

        throw ApiException.notFound("No se encontró el perfil de la cuenta.");
    }
}
