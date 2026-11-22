package com.example.demo.service;

import com.example.demo.entities.Address;
import com.example.demo.errors.ApiException;
import com.example.demo.entities.Client;
import com.example.demo.entities.Role;
import com.example.demo.repository.AdminRepository;
import com.example.demo.repository.ClientRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.repository.DeliveryPersonRepository;
import com.example.demo.repository.OperatorRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// Indica que esta clase contiene la lógica de negocio para gestionar clientes.
@Service
public class ClientServiceImpl implements ClientService {

    // Repositorio encargado de consultar y modificar los clientes.
    @Autowired
    private ClientRepository repository;

    // Repositorio utilizado para comprobar si un correo pertenece a un
    // administrador.
    @Autowired
    private AdminRepository adminRepository;

    // Repositorio utilizado para comprobar si un correo pertenece a un operador.
    @Autowired
    private OperatorRepository operatorRepository;

    // Repositorio utilizado para comprobar si un correo pertenece a un repartidor.
    @Autowired
    private DeliveryPersonRepository deliveryPersonRepository;

    // Servicio de cupones, para limpiar los cupones personales al borrar la cuenta.
    @Autowired
    private CouponService couponService;

    // Servicio de reseñas, para borrar la reseña al eliminar la cuenta.
    @Autowired
    private ReviewService reviewService;

    // Servicio encargado de gestionar los pedidos asociados a los clientes.
    @Autowired
    private OrderService orderService;

    // Maneja las credenciales en la tabla users
    @Autowired
    private UserAccountService userAccountService;

    // Envía el correo para confirmar la cuenta de los clientes nuevos
    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private AddressService addressService;

    // Servicio de suscripciones, para cerrar los pagos pendientes al borrar la
    // cuenta.
    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private NotificationService notificationService;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    // Obtiene todos los clientes registrados en la base de datos.
    @Override
    @Transactional
    public Collection<Client> getAllClients() {
        return repository.findAll();
    }

    // Busca un cliente por su ID; si no existe, genera un error 404.
    @Override
    @Transactional
    public Client getClientById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("No se encontró el cliente solicitado."));
    }

    // Registra un cliente después de validar su correo y comprobar que no existan
    // duplicados.
    @Override
    @Transactional
    public Client addClient(Client client) {

        // Verifica que el correo cumpla con las reglas establecidas.
        EmailRules.validate(client.getEmail());

        // El correo no puede estar usado por ningún tipo de usuario.
        if (repository.findByEmail(client.getEmail()) != null
                || adminRepository.findByEmail(client.getEmail()) != null
                || operatorRepository.findByEmail(client.getEmail()) != null
                || deliveryPersonRepository.findByEmail(client.getEmail()) != null) {
            throw ApiException.conflict(
                    "Ya existe una cuenta registrada con ese correo electrónico.");
        }

        // El teléfono es obligatorio (solo las cuentas de Google lo dejan para después)
        requirePhone(client);

        // Comprueba que el número de teléfono no esté registrado por otro cliente.
        if (repository.findByPhone(client.getPhone()) != null) {
            throw ApiException.conflict(
                    "Ya existe una cuenta registrada con ese número de teléfono.");
        }

        // Un cliente nuevo debe registrarse con al menos una dirección.
        if (client.getAddresses() == null || client.getAddresses().isEmpty()) {
            throw ApiException.badRequest("Agrega al menos una dirección.");
        }

        // Las direcciones llegan sin cliente (Address.client es @JsonIgnore):
        // se limpian y se enlazan al nuevo cliente antes de guardar.
        // La primera queda como predeterminada; sin nombre se llama "Casa".
        List<String> usedNames = new ArrayList<>();
        boolean first = true;
        for (Address address : client.getAddresses()) {
            address.setId(null);
            address.setLabel(AddressRules.clean(address.getLabel()));

            String name = AddressRules.cleanName(address.getName());
            if (name == null) {
                name = AddressRules.freeName(usedNames);
            }
            usedNames.add(name);
            address.setName(name);

            address.setDefaultAddress(first);
            first = false;

            address.setClient(client);
        }
        // El id y el rol los decide el servidor: así nadie se registra como ADMIN
        client.setId(null);
        client.setRole(Role.CLIENT);
        client.setPointsBalance(0);
        client.setSubscriptionEndsAt(null);

        // Crea la cuenta en users (correo + contraseña cifrada + rol CLIENT)
        client.setUser(userAccountService.createUser(client.getEmail(), client.getPassword(), Role.CLIENT, false));
        // Guarda el cliente en la base de datos
        Client saved = repository.save(client);

        // Toda cuenta nueva recibe su cupón de bienvenida
        couponService.createWelcomeCoupon(saved);

        notificationService.notifyWelcome(saved);

        // Envía el correo de confirmación (sale cuando la transacción se guarda)
        emailVerificationService.sendVerification(saved.getUser(), saved.getEmail(), saved.getName());

        return saved;
    }

    // Sin esto, findByPhone(null) encontraría las cuentas de Google sin teléfono
    private static void requirePhone(Client client) {
        if (client.getPhone() == null || client.getPhone().isBlank()) {
            throw ApiException.badRequest("Escribe tu número de teléfono.");
        }
        client.setPhone(client.getPhone().trim());
    }

    // Completa una cuenta creada con Google. Reutiliza las mismas reglas de
    // "Editar cuenta" (teléfono único) y de "Agregar dirección".
    @Override
    @Transactional
    public Client completeProfile(Integer id, com.example.demo.dto.CompleteProfileDTO data) {
        Client actual = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("El cliente no existe."));

        if (data == null) {
            throw ApiException.badRequest("Faltan los datos del perfil.");
        }

        String name = data.name() == null ? "" : data.name().trim();
        String lastName = data.lastName() == null ? "" : data.lastName().trim();
        String phone = data.phone() == null ? "" : data.phone().trim();

        if (name.isEmpty() || name.length() > 70) {
            throw ApiException.badRequest("Escribe tu nombre (máximo 70 caracteres).");
        }
        if (lastName.isEmpty() || lastName.length() > 70) {
            throw ApiException.badRequest("Escribe tu apellido (máximo 70 caracteres).");
        }
        if (phone.isEmpty() || phone.length() > 25) {
            throw ApiException.badRequest("Escribe tu número de teléfono.");
        }

        boolean needsAddress = actual.getAddresses().isEmpty();
        if (needsAddress && (data.address() == null
                || data.address().getLabel() == null
                || data.address().getLabel().isBlank())) {
            throw ApiException.badRequest("Agrega la dirección donde quieres recibir tus pedidos.");
        }

        Client saved = updateClient(id, Client.builder()
                .name(name)
                .lastName(lastName)
                .email(actual.getEmail())
                .phone(phone)
                .build());

        if (needsAddress) {
            addressService.addAddress(id, data.address());
            // La dirección se guardó por su repositorio: se recarga el cliente
            // para que isProfileComplete() la vea
            entityManager.flush();
            entityManager.refresh(saved);
        }
        return saved;
    }

    // Actualiza la información de un cliente existente.
    @Override
    @Transactional
    public Client updateClient(Integer id, Client client) {

        // Busca el cliente que se va a actualizar; si no existe, genera un error 404.
        Client actual = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("El cliente no existe."));

        // Valida el formato del correo recibido.
        EmailRules.validate(client.getEmail());
        requirePhone(client);

        // Consulta si el correo o el teléfono ya están registrados en otro cliente.
        Client emailClient = repository.findByEmail(client.getEmail());
        Client phoneClient = repository.findByPhone(client.getPhone());

        // Impide usar un teléfono que pertenezca a otro cliente.
        if (phoneClient != null && !phoneClient.getId().equals(id)) {
            throw ApiException.conflict(
                    "Ya existe otra cuenta con ese número de teléfono.");
        }

        // Impide usar un correo que pertenezca a otro cliente.
        if (emailClient != null
                && !emailClient.getId().equals(id)) {

            throw ApiException.conflict(
                    "Ya existe otra cuenta con ese correo.");
        }

        // Comprueba que el correo no esté registrado por un administrador.
        if (adminRepository.findByEmail(client.getEmail()) != null) {
            throw ApiException.conflict(
                    "Ya existe otra cuenta con ese correo.");
        }

        // Conserva el identificador del cliente que se está actualizando.
        client.setId(actual.getId());

        // El formulario de edición no envía "role", así que lo preservamos
        // igual que se preserva la contraseña cuando viene vacía.
        client.setRole(actual.getRole());

        // Se conserva el mismo usuario de la tabla users; si cambió el correo,
        // también se actualiza su username (que es con lo que inicia sesión).
        userAccountService.updateUsername(actual.getUser(), client.getEmail());
        client.setUser(actual.getUser());

        // Las direcciones se gestionan solo por /clients/me/addresses;
        // si no se conservan, orphanRemoval las borraría todas al guardar.
        client.setAddresses(actual.getAddresses());

        // Igual con la reserva (1 a 1): se gestiona solo por /clients/me/reservation
        client.setReservation(actual.getReservation());

        // La suscripción se gestiona solo por /subscriptions: editar el perfil
        // no debe borrarla (el cliente nuevo que llega no trae esta fecha)
        client.setSubscriptionEndsAt(actual.getSubscriptionEndsAt());
        client.setPointsBalance(actual.getPointsBalance());
        // Guarda los datos actualizados del cliente.
        return repository.save(client);
    }

    // Elimina un cliente y gestiona previamente las relaciones con sus pedidos.
    @Override
    @Transactional
    public void deleteClient(Integer id) {
        // Verifica que el cliente exista antes de intentar eliminarlo.
        if (!repository.existsById(id)) {
            throw ApiException.notFound("No se encontró el cliente que se desea eliminar.");
        }

        // Valida que no haya pedidos en curso y cancela los pendientes.
        orderService.cancelPendingOrdersForClientDeletion(id);

        // Antes de eliminar el cliente, limpiamos los id de los pedidos.
        orderService.clearClientIdFromOrders(id);

        // Cupones personales: se borran, o se desactivan si ya están en pedidos
        couponService.removeClientCoupons(id);

        // La reseña del cliente se borra antes que el cliente
        reviewService.deleteClientReview(id);

        // Pagos de suscripción: se cierra la sesión de Stripe abierta y se borran los
        // registros
        subscriptionService.deleteClientPayments(id);

        // Elimina el cliente de la base de datos.

        // Sus notificaciones se borran con la cuenta
        notificationService.deleteAll(id);
        repository.deleteById(id);
    }

    // Busca un cliente mediante su correo electrónico.
    @Override
    @Transactional
    public Client findByEmail(String email) {
        return repository.findByEmail(email);
    }

    // Cambia la contraseña después de comprobar que el cliente existe y validar las
    // credenciales.
    // Cambia la contraseña en la tabla users (comprueba primero la actual)
    @Override
    @Transactional
    public void changePassword(Integer id, String currentPassword, String newPassword) {
        Client client = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("No se encontró el cliente solicitado."));

        userAccountService.changePassword(client.getUser(), currentPassword, newPassword);
    }

}