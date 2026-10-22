package com.example.demo.service;

import com.example.demo.entities.Order;
import com.example.demo.entities.OrderStatus;
import java.util.List;
import com.example.demo.entities.Operator;
import com.example.demo.repository.AdminRepository;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.DeliveryPersonRepository;
import com.example.demo.repository.OperatorRepository;
import com.example.demo.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Service
public class OperatorServiceImpl implements OperatorService {

    @Autowired
    private OperatorRepository repository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private DeliveryPersonRepository deliveryPersonRepository;

    @Autowired
    private OrderRepository orderRepository;

    // Un pedido deja de estar activo cuando se entrega o se cancela
    private static final List<OrderStatus> FINISHED_STATUSES = List.of(
            OrderStatus.DELIVERED,
            OrderStatus.CANCELLED);

    @Override
    @Transactional
    public Collection<Operator> getAllOperators() {
        return repository.findAll();
    }

    @Override
    @Transactional
    public Operator getOperatorById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el operador solicitado."));
    }

    // El correo no puede estar usado por ningún tipo de usuario.
    // currentOperatorId permite que al editar un operador conserve su propio
    // correo.
    private void validateEmailAvailable(String email, Integer currentOperatorId) {
        Operator sameEmail = repository.findByEmail(email);
        boolean usedByOtherOperator = sameEmail != null && !sameEmail.getId().equals(currentOperatorId);

        if (usedByOtherOperator
                || adminRepository.findByEmail(email) != null
                || clientRepository.findByEmail(email) != null
                || deliveryPersonRepository.findByEmail(email) != null) {
            throw new IllegalArgumentException(
                    "Ya existe una cuenta registrada con ese correo electrónico.");
        }
    }

    // El nombre de usuario no puede estar usado por ningún operador.
    // currentOperatorId permite que al editar un operador conserve su propio nombre
    // de usuario.
    private void validateUsernameAvailable(String username, Integer currentOperatorId) {
        Operator sameUsername = repository.findByUsername(username);
        boolean usedByOtherOperator = sameUsername != null && !sameUsername.getId().equals(currentOperatorId);

        if (usedByOtherOperator) {
            throw new IllegalArgumentException(
                    "Ya existe una cuenta registrada con ese nombre de usuario.");
        }
    }

    @Override
    @Transactional
    public Operator addOperator(Operator operator) {
        validateEmailAvailable(operator.getEmail(), null);
        validateUsernameAvailable(operator.getUsername(), null);
        // Evita que un id enviado desde el front sobrescriba un operador existente
        operator.setId(null);
        if (operator.getActive() == null) {
            operator.setActive(true);
        }

        return repository.save(operator);
    }

    @Override
    @Transactional
    public Operator updateOperator(Integer id, Operator operator) {

        Operator existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El operador no existe."));

        validateEmailAvailable(operator.getEmail(), id);
        validateUsernameAvailable(operator.getUsername(), id);

        existing.setName(operator.getName());
        existing.setLastName(operator.getLastName());
        existing.setEmail(operator.getEmail());
        existing.setUsername(operator.getUsername());
        existing.setRole(operator.getRole());

        if (operator.getActive() != null) {
            existing.setActive(operator.getActive());
        }

        // Solo se cambia la contraseña si viene una nueva
        if (operator.getPassword() != null && !operator.getPassword().isBlank()) {
            existing.setPassword(operator.getPassword());
        }

        return repository.save(existing);
    }

    @Override
    @Transactional
    public void deleteOperator(Integer id) {

        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("No se encontró el operador que se desea eliminar.");
        }

        // Solo se bloquea si todavía tiene pedidos sin completar
        if (orderRepository.existsByOperatorIdAndStatusNotIn(id, FINISHED_STATUSES)) {
            throw new IllegalArgumentException(
                    "No se puede eliminar este operador porque tiene pedidos sin completar. Puedes desactivarlo.");
        }

        // Los pedidos ya terminados se conservan en el historial, sin operador
        List<Order> finishedOrders = orderRepository.findByOperatorId(id);
        for (Order order : finishedOrders) {
            order.setOperator(null);
        }
        orderRepository.saveAll(finishedOrders);

        repository.deleteById(id);
    }

    @Override
    @Transactional
    public Operator toggleOperatorActive(Integer id) {
        Operator operator = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el operador solicitado."));

        operator.setActive(!Boolean.TRUE.equals(operator.getActive()));

        return repository.save(operator);
    }

    @Override
    @Transactional
    public Operator findByEmail(String email) {
        return repository.findByEmail(email);
    }

    @Override
    @Transactional
    public void changePassword(Integer id, String currentPassword, String newPassword) {
        Operator operator = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el operador solicitado."));

        PasswordRules.validateChange(operator.getPassword(), currentPassword, newPassword);

        operator.setPassword(newPassword);
        repository.save(operator);
    }

}