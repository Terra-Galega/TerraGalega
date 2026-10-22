package com.example.demo.service;

import com.example.demo.entities.Order;
import com.example.demo.entities.OrderStatus;
import com.example.demo.entities.DeliveryPerson;
import com.example.demo.entities.Role;
import com.example.demo.repository.AdminRepository;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.DeliveryPersonRepository;
import com.example.demo.repository.OperatorRepository;
import com.example.demo.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DeliveryPersonServiceImpl implements DeliveryPersonService {

    @Autowired
    private DeliveryPersonRepository repository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OperatorRepository operatorRepository;

    @Autowired
    private OrderRepository orderRepository;

    // Un pedido deja de estar activo cuando se entrega o se cancela
    private static final List<OrderStatus> FINISHED_STATUSES = List.of(
            OrderStatus.DELIVERED,
            OrderStatus.CANCELLED);

    @Override
    @Transactional
    public List<DeliveryPerson> getAllDeliveryPersons() {
        return repository.findAll();
    }

    // Solo domiciliarios activos y que no estén en un domicilio
    @Override
    @Transactional
    public List<DeliveryPerson> getAvailableDeliveryPersons() {
        return repository.findByAvailableTrueAndActiveTrue();
    }

    @Override
    @Transactional
    public DeliveryPerson getDeliveryPersonById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el domiciliario solicitado."));
    }

    // Revisa que los datos obligatorios vengan completos. La contraseña solo
    // es obligatoria al crear; al editar, si viene vacía se conserva la actual.
    private void validateRequired(DeliveryPerson dp, boolean passwordRequired) {
        if (isBlank(dp.getName()) || isBlank(dp.getLastName()) || isBlank(dp.getEmail())
                || isBlank(dp.getIdentification()) || isBlank(dp.getPhone())) {
            throw new IllegalArgumentException("Todos los campos del domiciliario son obligatorios.");
        }

        if (passwordRequired && isBlank(dp.getPassword())) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }
    }

    // Correo, identificación y teléfono no pueden repetirse.
    // El correo tampoco puede estar usado por ningún otro tipo de usuario.
    // currentId permite que al editar conserve sus propios datos.
    private void validateUnique(DeliveryPerson dp, Integer currentId) {
        DeliveryPerson sameEmail = repository.findByEmail(dp.getEmail());
        boolean emailUsedByOtherDelivery = sameEmail != null && !sameEmail.getId().equals(currentId);

        if (emailUsedByOtherDelivery
                || adminRepository.findByEmail(dp.getEmail()) != null
                || clientRepository.findByEmail(dp.getEmail()) != null
                || operatorRepository.findByEmail(dp.getEmail()) != null) {
            throw new IllegalArgumentException(
                    "Ya existe una cuenta registrada con ese correo electrónico.");
        }

        DeliveryPerson sameIdentification = repository.findByIdentification(dp.getIdentification());
        if (sameIdentification != null && !sameIdentification.getId().equals(currentId)) {
            throw new IllegalArgumentException(
                    "Ya existe un domiciliario con esa identificación.");
        }

        DeliveryPerson samePhone = repository.findByPhone(dp.getPhone());
        if (samePhone != null && !samePhone.getId().equals(currentId)) {
            throw new IllegalArgumentException(
                    "Ya existe un domiciliario con ese teléfono.");
        }
    }

    @Override
    @Transactional
    public DeliveryPerson addDeliveryPerson(DeliveryPerson deliveryPerson) {

        validateRequired(deliveryPerson, true);
        validateUnique(deliveryPerson, null);

        // Evita que un id enviado desde el front sobrescriba un domiciliario existente
        deliveryPerson.setId(null);
        // El rol y la disponibilidad los define el servidor, no el front
        deliveryPerson.setRole(Role.DELIVERY_PERSON);
        deliveryPerson.setAvailable(true);
        if (deliveryPerson.getActive() == null) {
            deliveryPerson.setActive(true);
        }

        return repository.save(deliveryPerson);
    }

    @Override
    @Transactional
    public DeliveryPerson updateDeliveryPerson(Integer id, DeliveryPerson deliveryPerson) {

        DeliveryPerson existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El domiciliario no existe."));

        validateRequired(deliveryPerson, false);
        validateUnique(deliveryPerson, id);

        existing.setName(deliveryPerson.getName());
        existing.setLastName(deliveryPerson.getLastName());
        existing.setEmail(deliveryPerson.getEmail());
        existing.setIdentification(deliveryPerson.getIdentification());
        existing.setPhone(deliveryPerson.getPhone());

        if (deliveryPerson.getActive() != null) {
            existing.setActive(deliveryPerson.getActive());
        }

        // Solo se cambia la contraseña si viene una nueva.
        // "available" no se toca: lo controlan el envío y la entrega de pedidos.
        if (!isBlank(deliveryPerson.getPassword())) {
            existing.setPassword(deliveryPerson.getPassword());
        }

        return repository.save(existing);
    }

    // Activa o desactiva al domiciliario (por ejemplo si ese día no trabaja)
    @Override
    @Transactional
    public DeliveryPerson toggleDeliveryPersonActive(Integer id) {

        DeliveryPerson existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El domiciliario no existe."));

        existing.setActive(!Boolean.TRUE.equals(existing.getActive()));

        return repository.save(existing);
    }

    @Override
    @Transactional
    public void deleteDeliveryPerson(Integer id) {

        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("No se encontró el domiciliario que se desea eliminar.");
        }

        // Solo se bloquea si todavía tiene pedidos sin completar
        if (orderRepository.existsByDeliveryPersonIdAndStatusNotIn(id, FINISHED_STATUSES)) {
            throw new IllegalArgumentException(
                    "No se puede eliminar este domiciliario porque tiene pedidos sin completar. Puedes desactivarlo.");
        }

        // Los pedidos ya terminados se conservan en el historial, sin domiciliario
        List<Order> finishedOrders = orderRepository.findByDeliveryPersonId(id);
        for (Order order : finishedOrders) {
            order.setDeliveryPerson(null);
        }
        orderRepository.saveAll(finishedOrders);

        repository.deleteById(id);
    }

    @Override
    @Transactional
    public DeliveryPerson findByEmail(String email) {
        return repository.findByEmail(email);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}