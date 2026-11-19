package com.example.demo.security;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entities.DeliveryPerson;
import com.example.demo.entities.Operator;
import com.example.demo.entities.RoleEntity;
import com.example.demo.entities.UserEntity;
import com.example.demo.repository.DeliveryPersonRepository;
import com.example.demo.repository.OperatorRepository;
import com.example.demo.repository.UserRepository;

// Carga un usuario de la tabla users y lo convierte al formato que usa
// Spring Security (UserDetails): correo, contraseña cifrada, roles y si está activo.
// La usan el login, para comparar la contraseña, y el filtro JWT en cada petición.
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OperatorRepository operatorRepository;

    @Autowired
    private DeliveryPersonRepository deliveryPersonRepository;

    @Autowired
    private com.example.demo.repository.WaiterRepository waiterRepository;

    // En esta aplicación el username es el correo
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userDB = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return User.withUsername(userDB.getUsername())
                .password(userDB.getPassword())
                .authorities(mapRolesToAuthorities(userDB.getRoles()))
                // Una cuenta desactivada por el admin no puede iniciar sesión ni usar su token
                .disabled(!isActive(username))
                .build();
    }

    // Convierte los roles de la base de datos en roles de Spring Security
    private Collection<GrantedAuthority> mapRolesToAuthorities(List<RoleEntity> roles) {
        return roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role.getName()))
                .toList();
    }

    // Solo los operadores, los domiciliarios y los meseros se pueden desactivar
    private boolean isActive(String email) {
        Operator operator = operatorRepository.findByEmail(email);
        if (operator != null) {
            return Boolean.TRUE.equals(operator.getActive());
        }
        DeliveryPerson deliveryPerson = deliveryPersonRepository.findByEmail(email);
        if (deliveryPerson != null) {
            return Boolean.TRUE.equals(deliveryPerson.getActive());
        }
        com.example.demo.entities.Waiter waiter = waiterRepository.findByEmail(email);
        if (waiter != null) {
            return Boolean.TRUE.equals(waiter.getActive());
        }
        return true;
    }
}
