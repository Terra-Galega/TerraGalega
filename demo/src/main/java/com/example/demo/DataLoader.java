package com.example.demo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.example.demo.entities.AddOn;
import com.example.demo.entities.Address;
import com.example.demo.entities.Admin;
import com.example.demo.entities.Announcement;
import com.example.demo.entities.Category;
import com.example.demo.entities.Client;
import com.example.demo.entities.Coupon;
import com.example.demo.entities.DeliveryPerson;
import com.example.demo.entities.DiscountType;
import com.example.demo.entities.Operator;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderDetail;
import com.example.demo.entities.OrderDetailAddOn;
import com.example.demo.entities.OrderStatus;
import com.example.demo.entities.Product;
import com.example.demo.entities.Reservation;
import com.example.demo.entities.ReservationTheme;
import com.example.demo.entities.Review;
import com.example.demo.entities.Role;
import com.example.demo.entities.RoleEntity;
import com.example.demo.repository.RoleRepository;
import com.example.demo.service.UserAccountService;
import com.example.demo.repository.AddOnRepository;
import com.example.demo.repository.AdminRepository;
import com.example.demo.repository.AnnouncementRepository;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.CouponRepository;
import com.example.demo.repository.DeliveryPersonRepository;
import com.example.demo.repository.OperatorRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.DiningTableRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.ReviewRepository;
import jakarta.transaction.Transactional;

@Component
// Carga datos de demostración cada vez que arranca la aplicación
// (CommandLineRunner).
// Inserta sin comprobar si ya existen; no se duplica porque
// ddl-auto=create-drop recrea las tablas en cada arranque.
@Profile("default")
public class DataLoader implements CommandLineRunner {

        @Autowired
        private ClientRepository clientRepository;

        @Autowired
        private AdminRepository adminRepository;

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private CategoryRepository categoryRepository;

        @Autowired
        private AddOnRepository addOnRepository;

        @Autowired
        private DeliveryPersonRepository deliveryPersonRepository;

        @Autowired
        private OperatorRepository operatorRepository;

        @Autowired
        private OrderRepository orderRepository;

        @Autowired
        private CouponRepository couponRepository;

        @Autowired
        private AnnouncementRepository announcementRepository;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private ReviewRepository reviewRepository;

        @Autowired
        private ReservationRepository reservationRepository;

        // Las mesas las carga antes DiningTableLoader (@Order(0))
        @Autowired
        private DiningTableRepository diningTableRepository;

        // Crea las filas en users con la contraseña cifrada
        @Autowired
        private UserAccountService userAccountService;

        // --- Helpers para reutilizar los productos/addOns ya cargados arriba,
        // en vez de duplicar su creación ---
        private Product findProductByName(String name) {
                return productRepository.findAll().stream()
                                .filter(p -> p.getName().equals(name))
                                .findFirst()
                                .orElseThrow();
        }

        private AddOn findAddOnByName(String name) {
                return addOnRepository.findAll().stream()
                                .filter(a -> a.getName().equals(name))
                                .findFirst()
                                .orElseThrow();
        }

        private Set<Category> cats(Category... categories) {
                return new HashSet<>(Arrays.asList(categories));
        }

        // Cada helper crea primero la fila en users (correo + contraseña cifrada +
        // rol), la asocia por OneToOne y después guarda la entidad.
        // El .password(...) del builder es @Transient: solo sirve para crear el user.
        private Admin saveAdmin(Admin admin) {
                admin.setUser(userAccountService.createUser(admin.getEmail(), admin.getPassword(), Role.ADMIN, true));
                return adminRepository.save(admin);
        }

        private Operator saveOperator(Operator operator) {
                operator.setUser(userAccountService.createUser(operator.getEmail(), operator.getPassword(),
                                Role.OPERATOR, true));
                return operatorRepository.save(operator);
        }

        private DeliveryPerson saveDeliveryPerson(DeliveryPerson deliveryPerson) {
                deliveryPerson.setUser(userAccountService.createUser(deliveryPerson.getEmail(),
                                deliveryPerson.getPassword(), Role.DELIVERY_PERSON, true));
                return deliveryPersonRepository.save(deliveryPerson);
        }

        // Guarda un cliente junto con sus direcciones (se guardan por cascade).
        // La primera se llama "Casa" y es la predeterminada.
        private Client saveClient(Client client, String... addressLabels) {
                String[] names = { "Casa", "Oficina", "Otra" };
                for (int i = 0; i < addressLabels.length; i++) {
                        client.getAddresses().add(Address.builder()
                                        .name(i < names.length ? names[i] : "Dirección " + (i + 1))
                                        .label(addressLabels[i])
                                        .defaultAddress(i == 0)
                                        .client(client)
                                        .build());
                }
                client.setUser(userAccountService.createUser(client.getEmail(), client.getPassword(), Role.CLIENT,
                                true));
                return clientRepository.save(client);
        }

        @Override
        @Transactional
        public void run(String... args) throws Exception {
                // Roles primero: todos los usuarios dependen de ellos
                for (Role role : Role.values()) {
                        roleRepository.save(new RoleEntity(role.name()));
                }
                // Orden importante: clientes -> admins -> categorías -> addOns -> productos ->
                // domiciliarios -> operadores -> pedidos, porque cada grupo depende de los
                // anteriores.

                // Clientes: correos personales (gmail, outlook, hotmail, yahoo, icloud).
                // El dominio @terra.com queda reservado al personal: admins, operadores y
                // domiciliarios. Contraseña de cada uno: <nombre>123 (ver la lista).
                saveClient(
                                Client.builder().name("Juan").lastName("García").email("juan.garcia@gmail.com")
                                                .password("cliente123").phone("+57 300 214 7788")
                                                .role(Role.CLIENT).build(),
                                "Calle 10 #5-20, Bogotá", "Carrera 9 #72-15, Bogotá");
                saveClient(
                                Client.builder().name("Marta").lastName("Souto").email("marta.souto@outlook.com")
                                                .password("marta123").phone("+57 310 555 1122")
                                                .role(Role.CLIENT).build(),
                                "Carrera 15 #45-10, Bogotá");
                saveClient(
                                Client.builder().name("Diego").lastName("Pardo").email("diego.pardo@gmail.com")
                                                .password("diego123").phone("+57 320 444 9988")
                                                .role(Role.CLIENT)
                                                // Premium con 25 días restantes
                                                .subscriptionEndsAt(LocalDateTime.now().plusDays(25))
                                                .build(),
                                "Avenida 68 #20-33, Bogotá");
                saveClient(
                                Client.builder().name("María").lastName("López").email("maria.lopez@hotmail.com")
                                                .password("maria123").phone("+57 310 123 4567")
                                                .role(Role.CLIENT)
                                                // Premium que vence en 3 días (el botón debe permitir renovar)
                                                .subscriptionEndsAt(LocalDateTime.now().plusDays(3))
                                                .build(),
                                "Calle 72 #10-34, Bogotá");
                saveClient(
                                Client.builder().name("Carlos").lastName("Rodríguez")
                                                .email("carlos.rodriguez@gmail.com")
                                                .password("carlos123").phone("+57 315 987 6543")
                                                .role(Role.CLIENT)
                                                // Venció ayer: debe aparecer como freemium
                                                .subscriptionEndsAt(LocalDateTime.now().minusDays(1))
                                                .build(),
                                "Transversal 9 #100-2, Bogotá");
                saveClient(
                                Client.builder().name("Andrés").lastName("Martínez")
                                                .email("andres.martinez@outlook.com")
                                                .password("andres123").phone("+57 301 222 3344")
                                                .role(Role.CLIENT).build(),
                                "Carrera 7 #32-16, Bogotá");
                saveClient(
                                Client.builder().name("Laura").lastName("Ramírez").email("laura.ramirez@gmail.com")
                                                .password("laura123").phone("+57 311 666 7788")
                                                .role(Role.CLIENT).build(),
                                "Calle 85 #15-25, Bogotá");
                saveClient(
                                Client.builder().name("Sebastián").lastName("Torres")
                                                .email("sebastian.torres@yahoo.com")
                                                .password("sebastian123").phone("+57 322 111 2233")
                                                .role(Role.CLIENT).build(),
                                "Carrera 30 #45-03, Bogotá");
                saveClient(
                                Client.builder().name("Camila").lastName("Moreno").email("camila.moreno@gmail.com")
                                                .password("camila123").phone("+57 300 888 4455")
                                                .role(Role.CLIENT).build(),
                                "Calle 127 #19-60, Bogotá");
                saveClient(
                                Client.builder().name("Valeria").lastName("Castro").email("valeria.castro@gmail.com")
                                                .password("valeria123").phone("+57 312 409 2211")
                                                .role(Role.CLIENT).build(),
                                "Calle 93 #11-27, Bogotá");
                saveClient(
                                Client.builder().name("Felipe").lastName("Gutiérrez")
                                                .email("felipe.gutierrez@outlook.com")
                                                .password("felipe123").phone("+57 316 772 0198")
                                                .role(Role.CLIENT).build(),
                                "Carrera 11 #82-71, Bogotá");
                saveClient(
                                Client.builder().name("Natalia").lastName("Rojas").email("natalia.rojas@hotmail.com")
                                                .password("natalia123").phone("+57 304 550 6610")
                                                .role(Role.CLIENT).build(),
                                "Calle 116 #7-15, Bogotá");
                saveClient(
                                Client.builder().name("Juliana").lastName("Vargas").email("juliana.vargas@gmail.com")
                                                .password("juliana123").phone("+57 318 230 4471")
                                                .role(Role.CLIENT).build(),
                                "Carrera 13 #63-39, Bogotá");
                saveClient(
                                Client.builder().name("Santiago").lastName("Ospina").email("santiago.ospina@gmail.com")
                                                .password("santiago123").phone("+57 313 901 3378")
                                                .role(Role.CLIENT).build(),
                                "Avenida Calle 26 #59-41, Bogotá");
                saveClient(
                                Client.builder().name("Daniel").lastName("Quintero").email("daniel.quintero@yahoo.com")
                                                .password("daniel123").phone("+57 300 417 8820")
                                                .role(Role.CLIENT).build(),
                                "Calle 53 #21-40, Bogotá");
                saveClient(
                                Client.builder().name("Paula").lastName("Herrera").email("paula.herrera@icloud.com")
                                                .password("paula123").phone("+57 321 668 1094")
                                                .role(Role.CLIENT).build(),
                                "Carrera 19 #104-62, Bogotá");
                saveClient(
                                Client.builder().name("Alejandro").lastName("Navarro")
                                                .email("alejandro.navarro@gmail.com")
                                                .password("alejandro123").phone("+57 314 285 7731")
                                                .role(Role.CLIENT).build(),
                                "Calle 140 #10-21, Bogotá");
                saveClient(
                                Client.builder().name("Gabriela").lastName("Salazar")
                                                .email("gabriela.salazar@outlook.com")
                                                .password("gabriela123").phone("+57 305 712 9946")
                                                .role(Role.CLIENT).build(),
                                "Carrera 4 #69-12, Bogotá");
                saveClient(
                                Client.builder().name("Ricardo").lastName("Méndez").email("ricardo.mendez@gmail.com")
                                                .password("ricardo123").phone("+57 317 359 2287")
                                                .role(Role.CLIENT).build(),
                                "Calle 100 #19-54, Bogotá");
                saveClient(
                                Client.builder().name("Lucía").lastName("Pineda").email("lucia.pineda@hotmail.com")
                                                .password("lucia123").phone("+57 302 948 6612")
                                                .role(Role.CLIENT).build(),
                                "Carrera 50 #80-15, Bogotá");
                saveClient(
                                Client.builder().name("Esteban").lastName("Cárdenas")
                                                .email("esteban.cardenas@gmail.com")
                                                .password("esteban123").phone("+57 319 604 1158")
                                                .role(Role.CLIENT).build(),
                                "Calle 45 #13-70, Bogotá");
                saveClient(
                                Client.builder().name("Manuela").lastName("Ríos").email("manuela.rios@gmail.com")
                                                .password("manuela123").phone("+57 311 437 9005")
                                                .role(Role.CLIENT).build(),
                                "Carrera 21 #39-26, Bogotá");
                saveClient(
                                Client.builder().name("Héctor").lastName("Duarte").email("hector.duarte@outlook.com")
                                                .password("hector123").phone("+57 320 815 3364")
                                                .role(Role.CLIENT).build(),
                                "Calle 63 #9-83, Bogotá");
                saveClient(
                                Client.builder().name("Adriana").lastName("Beltrán").email("adriana.beltran@yahoo.com")
                                                .password("adriana123").phone("+57 310 276 5543")
                                                .role(Role.CLIENT).build(),
                                "Carrera 68D #24-19, Bogotá");
                saveClient(
                                Client.builder().name("Martín").lastName("Acosta").email("martin.acosta@gmail.com")
                                                .password("martin123").phone("+57 312 593 0427")
                                                .role(Role.CLIENT).build(),
                                "Calle 134 #55-30, Bogotá");
                saveClient(
                                Client.builder().name("Carolina").lastName("Suárez")
                                                .email("carolina.suarez@hotmail.com")
                                                .password("carolina123").phone("+57 315 128 6679")
                                                .role(Role.CLIENT).build(),
                                "Carrera 16 #96-64, Bogotá");
                saveClient(
                                Client.builder().name("Tomás").lastName("Villamil").email("tomas.villamil@gmail.com")
                                                .password("tomasv123").phone("+57 301 883 2140")
                                                .role(Role.CLIENT).build(),
                                "Calle 26 #4-49, Bogotá");
                saveClient(
                                Client.builder().name("Diana").lastName("Escobar").email("diana.escobar@outlook.com")
                                                .password("diana123").phone("+57 318 664 7712")
                                                .role(Role.CLIENT).build(),
                                "Carrera 15 #124-30, Bogotá");
                saveClient(
                                Client.builder().name("Samuel").lastName("Pacheco").email("samuel.pacheco@gmail.com")
                                                .password("samuel123").phone("+57 304 219 5583")
                                                .role(Role.CLIENT).build(),
                                "Calle 79 #8-38, Bogotá");
                saveClient(
                                Client.builder().name("Elena").lastName("Barrios").email("elena.barrios@icloud.com")
                                                .password("elena123").phone("+57 316 902 4436")
                                                .role(Role.CLIENT).build(),
                                "Carrera 7 #116-50, Bogotá");
                saveClient(
                                Client.builder().name("Miguel Ángel").lastName("Forero")
                                                .email("miguel.forero@gmail.com")
                                                .password("miguelf123").phone("+57 322 471 0985")
                                                .role(Role.CLIENT).build(),
                                "Calle 106 #54-15, Bogotá");
                saveClient(
                                Client.builder().name("Sara").lastName("Londoño").email("sara.londono@outlook.com")
                                                .password("sara123").phone("+57 313 350 8824")
                                                .role(Role.CLIENT).build(),
                                "Carrera 9 #80-45, Bogotá");

                // admin
                saveAdmin(
                                Admin.builder().name("Carlos").lastName("Martínez").email("admin@terra.com")
                                                .password("admin123").role(Role.ADMIN).build());
                saveAdmin(
                                Admin.builder().name("Ana").lastName("Fernández").email("ana.fernandez@terra.com")
                                                .password("ana123").role(Role.ADMIN).build());
                saveAdmin(
                                Admin.builder().name("Roberto").lastName("Silva").email("roberto.silva@terra.com")
                                                .password("roberto123").role(Role.ADMIN).build());
                saveAdmin(
                                Admin.builder().name("Patricia").lastName("Gómez").email("patricia.gomez@terra.com")
                                                .password("patricia123").role(Role.ADMIN).build());
                saveAdmin(
                                Admin.builder().name("Javier").lastName("Ortiz").email("javier.ortiz@terra.com")
                                                .password("javier123").role(Role.ADMIN).build());

                // Categories
                categoryRepository.save(Category.builder().name("Entradas")
                                .description("Pequeños platos gallegos pensados para abrir el apetito antes del plato principal.")
                                .build());

                categoryRepository.save(Category.builder().name("Mariscos")
                                .description("Lo mejor de la costa gallega: mariscos frescos preparados con recetas tradicionales.")
                                .build());

                categoryRepository.save(Category.builder().name("Carnes")
                                .description("Cortes a la brasa y guisos tradicionales de la cocina gallega.")
                                .build());

                categoryRepository.save(Category.builder().name("Postres")
                                .description("Dulces clásicos de Galicia para cerrar la comida con sabor a tierra.")
                                .build());
                categoryRepository.save(Category.builder().name("Bebidas")
                                .description(
                                                "Mixología, vinos y bebidas premium seleccionadas para elevar tu experiencia gastronómica.")
                                .build());
                categoryRepository.save(Category.builder().name("Maridajes")
                                .description("Vinos gallegos y tablas elegidos por nuestro sommelier para acompañar cada plato.")
                                .build());

                Category starters = categoryRepository.findByName("Entradas");
                Category seafood = categoryRepository.findByName("Mariscos");
                Category meats = categoryRepository.findByName("Carnes");
                Category desserts = categoryRepository.findByName("Postres");

                // AddOns
                addOnRepository.save(AddOn.builder().name("Ensalada verde")
                                .description("Adicional para entradas").price(7500.0).active(true)
                                .categories(cats(starters))
                                .build());
                addOnRepository.save(AddOn.builder().name("Extra ajo")
                                .description("Adicional para mariscos").price(3000.0).active(true)
                                .categories(cats(seafood))
                                .build());
                addOnRepository.save(AddOn.builder().name("Salsa de limón")
                                .description("Adicional para mariscos").price(4500.0).active(true)
                                .categories(cats(seafood))
                                .build());

                addOnRepository.save(AddOn.builder().name("Pan de millo")
                                .description("Adicional para carnes").price(5000.0).active(true).categories(cats(meats))
                                .build());
                addOnRepository.save(AddOn.builder().name("Cachelos")
                                .description("Adicional para carnes").price(9000.0).active(true).categories(cats(meats))
                                .build());

                addOnRepository.save(AddOn.builder().name("Nata montada")
                                .description("Adicional para postres").price(4500.0).active(true)
                                .categories(cats(desserts)).build());
                addOnRepository.save(AddOn.builder().name("Helado de vainilla")
                                .description("Adicional para postres").price(6000.0).active(true)
                                .categories(cats(desserts)).build());

                addOnRepository.save(AddOn.builder().name("Porción de Pan Rústico")
                                .description("Pan gallego artesanal, ideal para acompañar tus entradas y mojar salsas.")
                                .price(5000.0).active(true).categories(cats(starters)).build());

                addOnRepository.save(AddOn.builder().name("Extra Salsa Brava")
                                .description("Una porción extra de nuestra salsa brava ligeramente picante.")
                                .price(4000.0).active(true).categories(cats(starters)).build());

                addOnRepository.save(AddOn.builder().name("Picos Camperos")
                                .description("Palitos de pan crujientes típicos, perfectos para picar.")
                                .price(3000.0).active(true).categories(cats(starters)).build());

                // adicionales para mariscos
                addOnRepository.save(AddOn.builder().name("Ración de Cachelos")
                                .description("Patatas cocidas con pimentón, el acompañante perfecto para el pulpo.")
                                .price(9000.0).active(true).categories(cats(seafood)).build());

                addOnRepository.save(AddOn.builder().name("Salsa de Limón y Perejil")
                                .description("Toque cítrico y fresco extra para tus mariscos y pescados.")
                                .price(4500.0).active(true).categories(cats(seafood)).build());

                addOnRepository.save(AddOn.builder().name("Mayonesa Casera Suave")
                                .description("Nuestra mayonesa tradicional hecha en casa, sin ajo.")
                                .price(4000.0).active(true).categories(cats(seafood)).build());

                // adicionales para carnes
                addOnRepository.save(AddOn.builder().name("Ensalada Verde de Guarnición")
                                .description("Fresca mezcla de lechugas para equilibrar tus carnes.")
                                .price(7500.0).active(true).categories(cats(meats)).build());

                addOnRepository.save(AddOn.builder().name("Patatas Panaderas")
                                .description("Patatas horneadas lentamente a fuego lento, ideales para carnes asadas.")
                                .price(9000.0).active(true).categories(cats(meats)).build());

                addOnRepository.save(AddOn.builder().name("Puré de Patatas Trufado")
                                .description("Cremoso puré de patata con un toque de aceite de trufa blanca.")
                                .price(10000.0).active(true).categories(cats(meats)).build());

                // adicionales para postres
                addOnRepository.save(AddOn.builder().name("Bola de Helado de Vainilla")
                                .description("Combina perfecto con torrijas, tartas o postres calientes.")
                                .price(7000.0).active(true).categories(cats(desserts)).build());

                addOnRepository.save(AddOn.builder().name("Nata Montada Extra")
                                .description("Nata fresca montada al momento, ideal para acompañar dulces.")
                                .price(4500.0).active(true).categories(cats(desserts)).build());

                // Products

                productRepository.save(
                                Product.builder().name("Tortilla Española")
                                                .description("Tortilla tradicional de patatas y cebolla, jugosa por dentro y dorada por fuera.")
                                                .price(32000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://images.cookforyourlife.org/wp-content/uploads/2018/05/Tortilla-Espanola-1-696x464.jpg")
                                                .active(true).popular(false).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Patatas Bravas")
                                                .description("Patatas crujientes acompañadas de salsa brava casera ligeramente picante.")
                                                .price(28000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://www.foxandbriar.com/wp-content/uploads/2016/03/patatas-bravas-4-of-10.jpg")
                                                .active(true).popular(false).vegetarian(true).spicyMild(true)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Calamares a la Romana")
                                                .description("Anillas de calamar rebozadas y fritas hasta quedar doradas y crujientes.")
                                                .price(50000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://newluxbrand.com/recetas/wp-content/uploads/2022/02/calamares-a-la-romana-Newlux.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(true)
                                                .containsGluten(true).build());
                productRepository.save(
                                Product.builder().name("Gambas al Ajillo")
                                                .description("Gambas salteadas en aceite de oliva con ajo, guindilla y perejil fresco.")
                                                .price(56000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://invitadoinvierno.com/wp-content/uploads/2019/07/receta-gambas-ajillo-0.jpg")
                                                .active(true).popular(true).vegetarian(false).spicyMild(true)
                                                .spicyHot(true).containsNuts(false).containsSeafood(true)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Pulpo a la Brasa")
                                                .description("Tentáculo de pulpo a la brasa acompañado de patatas confitadas y aceite de oliva.")
                                                .price(74000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://esenciadelmar.es/wp-content/uploads/2023/08/pulpo-brasa-parilla-1200x700.jpeg")
                                                .active(true).popular(true).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(true)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Arroz Negro")
                                                .description("Arroz meloso cocinado con tinta de calamar, sepia, gambas y alioli casero.")
                                                .price(85000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://assets.tmecosys.com/image/upload/t_web_rdp_recipe_584x480/img/recipe/ras/Assets/f7f1b6c8-1a85-435e-b136-1e4cfa082903/Derivates/fbb52d55-2deb-4480-ac79-08b3522e4b09.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(true)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Fideuá Valenciana")
                                                .description("Fideos tostados cocinados con caldo de pescado, gambas, calamares y alioli.")
                                                .price(91000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://i0.wp.com/spainonafork.com/wp-content/uploads/2018/10/fideua3-11.png?fit=750%2C750&ssl=1")
                                                .active(true).popular(true).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(true)
                                                .containsGluten(true).build());

                productRepository.save(
                                Product.builder().name("Bacalao al Pil Pil")
                                                .description("Lomo de bacalao confitado en aceite de oliva con emulsión de ajo y guindilla.")
                                                .price(81000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://www.aceitesdeolivadeespana.com/wp-content/uploads/2020/09/bacalao-al-pil-pil.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(true)
                                                .spicyHot(false).containsNuts(false).containsSeafood(true)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Merluza a la Vasca")
                                                .description("Merluza fresca cocinada con salsa verde, almejas, espárragos y perejil.")
                                                .price(78000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRDV3yWMquqJyKwvVQG3hdrJ445tIeQrDw-3qUNt6Pox0VNHIWKldCADtz1&s=10")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(true)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Dorada al Horno")
                                                .description("Dorada fresca al horno con patatas, cebolla, limón y aceite de oliva virgen extra.")
                                                .price(69000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://lacocinadefrabisa.lavozdegalicia.es/wp-content/uploads/2016/03/dorada.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(true)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Cochinillo Segoviano")
                                                .description("Cochinillo asado lentamente hasta conseguir una piel crujiente y una carne tierna.")
                                                .price(120000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://www.tabladillo.es/wp-content/uploads/2024/07/Cochinillo-Tabladillo-Segovia-scaled.jpg")
                                                .active(true).popular(true).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Cordero Asado")
                                                .description("Pierna de cordero asada lentamente con hierbas aromáticas y patatas panaderas.")
                                                .price(109000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://assets.tmecosys.com/image/upload/t_web_rdp_recipe_584x480/img/recipe/ras/Assets/1C6B162D-43E7-4A9B-8690-3F873A07ED29/Derivates/D494B6BC-3836-410B-8455-B4555AAA565C.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Carrilleras de Cerdo")
                                                .description("Carrilleras de cerdo cocinadas a fuego lento en salsa de vino tinto y verduras.")
                                                .price(85000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://recetasdecocina.elmundo.es/wp-content/uploads/2025/12/carrilleras-de-cerdo-al-vino-tinto-1024x683.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Chorizo a la Sidra")
                                                .description("Chorizo asturiano cocinado lentamente en sidra natural hasta quedar jugoso y aromático.")
                                                .price(38000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://www.carolinescooking.com/wp-content/uploads/2023/06/chorizo-in-cider-featured-pic-sq.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Fabada Asturiana")
                                                .description("Guiso tradicional de fabes asturianas con chorizo, morcilla y panceta.")
                                                .price(67000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQbZY74kJkgVjohhJe6QHd8CaUV_q-J4ZjYtVNbUX7JgiDvQMDbrUbPh04&s=10")
                                                .active(true).popular(true).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Callos a la Madrileña")
                                                .description("Guiso tradicional de callos de ternera con chorizo, morcilla y especias.")
                                                .price(63000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://static.bainet.es/clip/9932fc94-4a07-48d9-b06d-eb170c7f222c_source-aspect-ratio_1600w_0.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Rabo de Toro")
                                                .description("Estofado de rabo de toro cocinado lentamente con vino tinto, verduras y especias.")
                                                .price(102000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://www.foodandwine.com/thmb/1qa4CivgyDuYGgHVAM_0GYXkQgw=/1500x0/filters:no_upscale():max_bytes(150000):strip_icc()/rabo-de-toro-XL-RECIPE0917-5f241cba48de485b935f0e882b504fe5.jpg")
                                                .active(true).popular(true).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Pollo al Ajillo")
                                                .description("Pollo dorado en aceite de oliva con abundante ajo, vino blanco y perejil.")
                                                .price(56000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://cookingtheglobe.com/wp-content/uploads/2016/03/pollo-al-ajillo-garlic-chicken-recipe-2.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Secreto Ibérico")
                                                .description("Corte de cerdo ibérico a la parrilla servido con patatas y reducción de vino tinto.")
                                                .price(97000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://assets.tmecosys.com/image/upload/t_web_rdp_recipe_584x480/img/recipe/ras/Assets/58BEF3B8-6B5A-452E-9A4D-CEB1E38098A6/Derivates/c1a17709-3a16-4fa1-8c71-937c04747466.jpg")
                                                .active(true).popular(true).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Migas Extremeñas")
                                                .description("Migas de pan salteadas con ajo, pimentón, chorizo y panceta al estilo extremeño.")
                                                .price(50000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://recetasdecocina.elmundo.es/wp-content/uploads/2024/11/migas-extremenas-receta-1024x683.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(true).build());

                productRepository.save(
                                Product.builder().name("Gazpacho Andaluz")
                                                .description("Sopa fría tradicional de tomate, pepino, pimiento, ajo y aceite de oliva.")
                                                .price(26000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://es-mycooktouch.group-taurus.com/image/recipe/540x391/gazpacho-andaluz")
                                                .active(true).popular(false).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Salmorejo Cordobés")
                                                .description("Crema fría de tomate y pan acompañada de huevo cocido y jamón serrano.")
                                                .price(30000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://www.aceitesdeolivadeespana.com/wp-content/uploads/2024/04/salmorejo_jamon_iberico.jpeg")
                                                .active(true).popular(true).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(true).build());

                productRepository.save(
                                Product.builder().name("Pimientos del Piquillo Rellenos")
                                                .description("Pimientos del piquillo rellenos de bacalao y cubiertos con salsa de pimientos.")
                                                .price(46000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTq0RRc3EIfGd43qP4eJR-7bikx3GGdDG02z0DZkU4mt9U1gMqEQRFKZ9u7&s=10")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(true)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Berenjenas con Miel")
                                                .description("Berenjenas crujientes acompañadas de miel de caña y una pizca de sal marina.")
                                                .price(34000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQq2dnL2PshnrGDZfPPIZG-9l5EhRmdsfVZkBUUmKoE3w&s=10")
                                                .active(true).popular(false).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Ensaladilla Rusa")
                                                .description("Ensaladilla tradicional de patata, zanahoria, guisantes, huevo y atún con mayonesa casera.")
                                                .price(32000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://www.seriouseats.com/thmb/Qj2Ta8m0V7eHoqB0Z6Z1eFD3D-I=/1500x0/filters:no_upscale():max_bytes(150000):strip_icc()/20260515-SEA-RussianSalad-AmandaSuarez-15-7f6b215dd4434cf7aec5f3095cd09ed1.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(true)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Boquerones en Vinagre")
                                                .description("Boquerones marinados en vinagre con ajo, perejil y aceite de oliva virgen extra.")
                                                .price(37000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://img.turkishstylecooking.com/wp-content/uploads/2025/01/boquerones_en_vinagre1.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(true)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Pisto Manchego")
                                                .description("Guiso de tomate, calabacín, pimiento, cebolla y berenjena con aceite de oliva.")
                                                .price(38000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://www.aceitesdeolivadeespana.com/wp-content/uploads/2021/02/pisto-manchego-receta.jpg")
                                                .active(true).popular(false).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Huevos Rotos con Jamón")
                                                .description("Huevos fritos sobre patatas caseras acompañados de láminas de jamón ibérico.")
                                                .price(51000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSol8_W3dxVbXfF6nucJj4zoDIE2LXJwddqEjHa8WdfQw&s=10")
                                                .active(true).popular(true).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Bocadillo de Calamares")
                                                .description("Tradicional bocadillo madrileño de calamares fritos servido con alioli casero.")
                                                .price(41000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://www.tiaalia.com/wp-content/uploads/2017/11/bocadillo-de-calamares.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(true)
                                                .containsGluten(true).build());

                productRepository.save(
                                Product.builder().name("Cachopo Asturiano")
                                                .description("Filetes de ternera rellenos de jamón serrano y queso, empanados y fritos hasta quedar dorados.")
                                                .price(91000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://tablasdelcampillin.com/wp-content/uploads/2023/05/cachopos-capsa_4aH-Editar.jpg")
                                                .active(true).popular(true).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(true).build());

                productRepository.save(
                                Product.builder().name("Crema Catalana")
                                                .description("Postre tradicional catalán de crema suave con una fina capa de azúcar caramelizado.")
                                                .price(28000.0)
                                                .category(categoryRepository.findByName("Postres"))
                                                .imageUrl("https://assets.tmecosys.com/image/upload/t_web_rdp_recipe_584x480/img/recipe/ras/Assets/2d814c07a2145d881d1e701d4b27c029/Derivates/93949c61852021496362279d132f6996ec0ac30b.jpg")
                                                .active(true).popular(false).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Arroz con Leche")
                                                .description("Postre cremoso de arroz cocido lentamente con leche, canela y cáscara de limón.")
                                                .price(25000.0)
                                                .category(categoryRepository.findByName("Postres"))
                                                .imageUrl("https://recetasdecocina.elmundo.es/wp-content/uploads/2024/11/arroz-con-leche-1024x683.jpg")
                                                .active(true).popular(false).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Churros con Chocolate")
                                                .description("Churros españoles crujientes acompañados de una taza de chocolate caliente y espeso.")
                                                .price(32000.0)
                                                .category(categoryRepository.findByName("Postres"))
                                                .imageUrl("https://www.realsimple.com/thmb/74r1RaSWyOqGyLD9vakiGkmyIYQ=/1500x0/filters:no_upscale():max_bytes(150000):strip_icc()/churros-chocolate-0f3587a6a59f4696af610cafe100d1fe.jpg")
                                                .active(true).popular(true).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(true).build());

                productRepository.save(
                                Product.builder().name("Torrijas")
                                                .description("Rebanadas de pan empapadas en leche aromatizada con canela y limón, doradas y espolvoreadas con azúcar.")
                                                .price(26000.0)
                                                .category(categoryRepository.findByName("Postres"))
                                                .imageUrl("https://images.aws.nestle.recipes/resized/2024_10_28T12_32_34_badun_images.badun.es_dd1722b7f5d9_torrija_con_helado_de_torrija_y_coulis_de_frutos_rojos_1290_742.jpg")
                                                .active(true).popular(false).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(true).build());

                productRepository.save(
                                Product.builder().name("Tarta de Queso Vasca")
                                                .description("Tarta de queso cremosa de interior suave y superficie caramelizada al estilo vasco.")
                                                .price(34000.0)
                                                .category(categoryRepository.findByName("Postres"))
                                                .imageUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSTPsxvBLFVs3SLnnc7A8F7_spaek91-Ghw1M3E8vb0ug&s=10")
                                                .active(true).popular(true).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Flan de Huevo")
                                                .description("Flan casero de huevo y leche servido con caramelo líquido y una textura delicadamente cremosa.")
                                                .price(25000.0)
                                                .category(categoryRepository.findByName("Postres"))
                                                .imageUrl("https://static.bainet.es/clip/9c993f21-16d2-4148-bece-ba68ae1e172a_source-aspect-ratio_1600w_0.jpg")
                                                .active(true).popular(false).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Sangría Española")
                                                .description("Bebida tradicional española preparada con vino tinto, frutas frescas y un toque cítrico.")
                                                .price(32000.0)
                                                .category(categoryRepository.findByName("Bebidas"))
                                                .imageUrl("https://descorcha.com/cdn/shop/articles/17380724881149_089e6f94-47d6-4852-b73d-a147c7542d85.jpg?v=1765379043")
                                                .active(true).popular(true).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Horchata de Chufa")
                                                .description("Bebida tradicional valenciana elaborada a base de chufa, fresca y ligeramente dulce.")
                                                .price(21000.0)
                                                .category(categoryRepository.findByName("Bebidas"))
                                                .imageUrl("https://www.finedininglovers.es/sites/default/files/styles/1_1_768x768/public/recipe_content_images/horchata-de-chufa%C2%A9iStock.jpg.webp?itok=5pehvI1m")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Tinto de Verano")
                                                .description("Bebida refrescante preparada con vino tinto, gaseosa y un toque de limón.")
                                                .price(25000.0)
                                                .category(categoryRepository.findByName("Bebidas"))
                                                .imageUrl("https://www.pequerecetas.com/wp-content/uploads/2022/06/como-hacer-tinto-de-verano.jpeg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Café Cortado")
                                                .description("Café espresso servido con una pequeña cantidad de leche caliente.")
                                                .price(14000.0)
                                                .category(categoryRepository.findByName("Bebidas"))
                                                .imageUrl("https://blogdelcafe.com/wp-content/uploads/2025/08/cortado-1024x647.webp")
                                                .active(true).popular(true).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                // Maridajes: vinos gallegos pensados para acompañar los platos de la carta
                productRepository.save(
                                Product.builder().name("Menú Maridaje Degustación")
                                                .description("Tres copas elegidas por el sommelier para acompañar entrada, principal y postre: Albariño, Mencía y Ribeiro dulce.")
                                                .price(115000.0)
                                                .category(categoryRepository.findByName("Maridajes"))
                                                .imageUrl("https://images.unsplash.com/photo-1510812431401-41d2bd2722f3?w=800&q=80")
                                                .active(true).popular(true).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());
                productRepository.save(
                                Product.builder().name("Maridaje Albariño y Mariscos")
                                                .description("Copa de Albariño Rías Baixas, fresco y salino, pensado para el pulpo, las gambas y las vieiras.")
                                                .price(42000.0)
                                                .category(categoryRepository.findByName("Maridajes"))
                                                .imageUrl("https://images.unsplash.com/photo-1547595628-c61a29f496f0?w=800&q=80")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());
                productRepository.save(
                                Product.builder().name("Maridaje Godello y Pescados")
                                                .description("Copa de Godello de Valdeorras, untuoso y frutal, ideal con merluza, bacalao y dorada.")
                                                .price(40000.0)
                                                .category(categoryRepository.findByName("Maridajes"))
                                                .imageUrl("https://images.unsplash.com/photo-1506377247377-2a5b3b417ebb?w=800&q=80")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());
                productRepository.save(
                                Product.builder().name("Maridaje Mencía y Carnes")
                                                .description("Copa de Mencía de la Ribeira Sacra para el cordero, el cochinillo y los guisos de la casa.")
                                                .price(46000.0)
                                                .category(categoryRepository.findByName("Maridajes"))
                                                .imageUrl("https://images.unsplash.com/photo-1553361371-9b22f78e8b1d?w=800&q=80")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());
                productRepository.save(
                                Product.builder().name("Maridaje de Quesos Gallegos")
                                                .description("Tabla de Tetilla, San Simón da Costa y Arzúa-Ulloa con membrillo, nueces y una copa de Ribeiro tinto.")
                                                .price(68000.0)
                                                .category(categoryRepository.findByName("Maridajes"))
                                                .imageUrl("https://images.unsplash.com/photo-1452195100486-9cc805987862?w=800&q=80")
                                                .active(true).popular(false).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(true).containsSeafood(false)
                                                .containsGluten(false).build());
                productRepository.save(
                                Product.builder().name("Maridaje Ribeiro y Postres")
                                                .description("Copa de Ribeiro dulce para acompañar la tarta de queso, la crema catalana o las torrijas.")
                                                .price(38000.0)
                                                .category(categoryRepository.findByName("Maridajes"))
                                                .imageUrl("https://images.unsplash.com/photo-1474722883778-792e7990302f?w=800&q=80")
                                                .active(true).popular(false).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());
                productRepository.save(
                                Product.builder().name("Selección del Sommelier")
                                                .description("Botella de la bodega gallega que recomienda el sommelier según los platos de tu mesa.")
                                                .price(160000.0)
                                                .category(categoryRepository.findByName("Maridajes"))
                                                .imageUrl("https://images.unsplash.com/photo-1516594915697-87eb3b1c14ea?w=800&q=80")
                                                .active(true).popular(false).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                // DeliveryPersons
                // available=false solo si llevan un pedido en camino (Luisa); un repartidor
                // inactivo (Tomás) tampoco está disponible.
                DeliveryPerson deliveryPerson1 = saveDeliveryPerson(
                                DeliveryPerson.builder().name("Pedro").lastName("Sánchez")
                                                .email("pedro.sanchez@terra.com").password("pedro123")
                                                .role(Role.DELIVERY_PERSON).identification("1001234567")
                                                .phone("+57 301 500 1001").available(true).active(true).build());
                DeliveryPerson deliveryPerson2 = saveDeliveryPerson(
                                DeliveryPerson.builder().name("Luisa").lastName("Fernández")
                                                .email("luisa.fernandez@terra.com").password("luisa123")
                                                .role(Role.DELIVERY_PERSON).identification("1001234568")
                                                .phone("+57 301 500 1002").available(false).active(true).build());
                saveDeliveryPerson(
                                DeliveryPerson.builder().name("Miguel Ángel").lastName("Ruiz")
                                                .email("miguel.ruiz@terra.com").password("miguel123")
                                                .role(Role.DELIVERY_PERSON).identification("1001234569")
                                                .phone("+57 301 500 1003").available(true).active(true).build());
                saveDeliveryPerson(
                                DeliveryPerson.builder().name("Daniela").lastName("Vargas")
                                                .email("daniela.vargas@terra.com").password("daniela123")
                                                .role(Role.DELIVERY_PERSON).identification("1001234570")
                                                .phone("+57 301 500 1004").available(true).active(true).build());
                saveDeliveryPerson(
                                DeliveryPerson.builder().name("Tomás").lastName("Herrera")
                                                .email("tomas.herrera@terra.com").password("tomas123")
                                                .role(Role.DELIVERY_PERSON).identification("1001234571")
                                                .phone("+57 301 500 1005").available(false).active(false).build());

                // Los operadores deben existir antes de los pedidos, que los referencian.
                // operator5 está inactivo (active=false).
                // Operators
                Operator operator1 = saveOperator(
                                Operator.builder().name("Sofía").lastName("Jiménez")
                                                .email("sofia.jimenez@terra.com").password("sofia123")
                                                .role(Role.OPERATOR).username("sofiaj").active(true).build());
                Operator operator2 = saveOperator(
                                Operator.builder().name("Mateo").lastName("Restrepo")
                                                .email("mateo.restrepo@terra.com").password("mateo123")
                                                .role(Role.OPERATOR).username("mateor").active(true).build());
                Operator operator3 = saveOperator(
                                Operator.builder().name("Valentina").lastName("Cruz")
                                                .email("valentina.cruz@terra.com").password("valentina123")
                                                .role(Role.OPERATOR).username("valentinac").active(true).build());
                Operator operator4 = saveOperator(
                                Operator.builder().name("Nicolás").lastName("Peña")
                                                .email("nicolas.pena@terra.com").password("nicolas123")
                                                .role(Role.OPERATOR).username("nicolasp").active(true).build());
                Operator operator5 = saveOperator(
                                Operator.builder().name("Isabella").lastName("Mendoza")
                                                .email("isabella.mendoza@terra.com").password("isabella123")
                                                .role(Role.OPERATOR).username("isabellam").active(false).build());

                // Orders (usando los clientes, productos, addOns, operadores y
                // domiciliarios ya existentes)
                // Se recuperan por email (único) para obtener las entidades ya guardadas.
                Client client1 = clientRepository.findByEmail("juan.garcia@gmail.com");
                Client client2 = clientRepository.findByEmail("marta.souto@outlook.com");
                Client client3 = clientRepository.findByEmail("diego.pardo@gmail.com");
                Client client4 = clientRepository.findByEmail("maria.lopez@hotmail.com");
                Client client5 = clientRepository.findByEmail("carlos.rodriguez@gmail.com");

                // Las fechas son relativas a hoy para que los estados tengan sentido (un pedido
                // "en camino" es de hace minutos, no de meses).
                LocalDateTime now = LocalDateTime.now();

                // Pedido 1 - entregado
                // Los detalles se enlazan en ambos sentidos (detalle -> pedido y
                // pedido.getDetails()); el unitPrice se copia del producto al momento del
                // pedido.
                Product tortilla = findProductByName("Tortilla Española");
                Product shrimp = findProductByName("Gambas al Ajillo");
                Order order1 = Order.builder()
                                .client(client1)
                                .status(OrderStatus.DELIVERED)
                                .createdAt(now.minusDays(2))
                                .deliveredAt(now.minusDays(2).plusMinutes(45))
                                .operator(operator1)
                                .deliveryPerson(deliveryPerson1)
                                .address(client1.getAddresses().get(0).getLabel())
                                .build();
                OrderDetail order1Detail1 = OrderDetail.builder()
                                .order(order1).product(tortilla).quantity(2)
                                .unitPrice(tortilla.getPrice()).build();
                OrderDetail order1Detail2 = OrderDetail.builder()
                                .order(order1).product(shrimp).quantity(1)
                                .unitPrice(shrimp.getPrice()).build();
                order1Detail2.getOrderDetailAddOns().add(
                                OrderDetailAddOn.builder().orderDetail(order1Detail2)
                                                .addOn(findAddOnByName("Extra ajo")).build());
                order1.getDetails().add(order1Detail1);
                order1.getDetails().add(order1Detail2);
                orderRepository.save(order1);

                // Pedido 2 - en camino
                Product octopus = findProductByName("Pulpo a la Brasa");
                Product sangria = findProductByName("Sangría Española");
                Order order2 = Order.builder()
                                .client(client2)
                                .status(OrderStatus.ON_THE_WAY)
                                .createdAt(now.minusMinutes(35))
                                .operator(operator2)
                                .deliveryPerson(deliveryPerson2)
                                .address(client2.getAddresses().get(0).getLabel())
                                .build();
                OrderDetail order2Detail1 = OrderDetail.builder()
                                .order(order2).product(octopus).quantity(1)
                                .unitPrice(octopus.getPrice()).build();
                OrderDetail order2Detail2 = OrderDetail.builder()
                                .order(order2).product(sangria).quantity(2)
                                .unitPrice(sangria.getPrice()).build();
                order2Detail2.getOrderDetailAddOns().add(
                                OrderDetailAddOn.builder().orderDetail(order2Detail2)
                                                .addOn(findAddOnByName("Salsa de limón")).build());
                order2.getDetails().add(order2Detail1);
                order2.getDetails().add(order2Detail2);
                orderRepository.save(order2);

                // Pedido 3 - en preparación (sin domiciliario asignado aún)
                Product stuffedVealCutlet = findProductByName("Cachopo Asturiano");
                Product catalanCream = findProductByName("Crema Catalana");
                Order order3 = Order.builder()
                                .client(client3)
                                .status(OrderStatus.PREPARING)
                                .createdAt(now.minusMinutes(20))
                                .operator(operator3)
                                .address(client3.getAddresses().get(0).getLabel())
                                .build();
                OrderDetail order3Detail1 = OrderDetail.builder()
                                .order(order3).product(stuffedVealCutlet).quantity(1)
                                .unitPrice(stuffedVealCutlet.getPrice()).build();
                OrderDetail order3Detail2 = OrderDetail.builder()
                                .order(order3).product(catalanCream).quantity(2)
                                .unitPrice(catalanCream.getPrice()).build();
                order3Detail1.getOrderDetailAddOns().add(
                                OrderDetailAddOn.builder().orderDetail(order3Detail1)
                                                .addOn(findAddOnByName("Pan de millo")).build());
                order3.getDetails().add(order3Detail1);
                order3.getDetails().add(order3Detail2);
                orderRepository.save(order3);

                // Pedido 4 - confirmado (sin domiciliario asignado aún)
                Product bravasPotatoes = findProductByName("Patatas Bravas");
                Product cortadoCoffee = findProductByName("Café Cortado");
                Order order4 = Order.builder()
                                .client(client4)
                                .status(OrderStatus.CONFIRMED)
                                .createdAt(now.minusMinutes(5))
                                .operator(operator4)
                                .address(client4.getAddresses().get(0).getLabel())
                                .build();
                OrderDetail order4Detail1 = OrderDetail.builder()
                                .order(order4).product(bravasPotatoes).quantity(3)
                                .unitPrice(bravasPotatoes.getPrice()).build();
                OrderDetail order4Detail2 = OrderDetail.builder()
                                .order(order4).product(cortadoCoffee).quantity(2)
                                .unitPrice(cortadoCoffee.getPrice()).build();
                order4Detail1.getOrderDetailAddOns().add(
                                OrderDetailAddOn.builder().orderDetail(order4Detail1)
                                                .addOn(findAddOnByName("Ensalada verde")).build());
                order4.getDetails().add(order4Detail1);
                order4.getDetails().add(order4Detail2);
                orderRepository.save(order4);

                // Pedido 5 - cancelado
                Product churros = findProductByName("Churros con Chocolate");
                Product horchata = findProductByName("Horchata de Chufa");
                Order order5 = Order.builder()
                                .client(client5)
                                .status(OrderStatus.CANCELLED)
                                .createdAt(now.minusDays(1))
                                .operator(operator5)
                                .address(client5.getAddresses().get(0).getLabel())
                                .build();
                OrderDetail order5Detail1 = OrderDetail.builder()
                                .order(order5).product(churros).quantity(1)
                                .unitPrice(churros.getPrice()).build();
                OrderDetail order5Detail2 = OrderDetail.builder()
                                .order(order5).product(horchata).quantity(1)
                                .unitPrice(horchata.getPrice()).build();
                order5Detail1.getOrderDetailAddOns().add(
                                OrderDetailAddOn.builder().orderDetail(order5Detail1)
                                                .addOn(findAddOnByName("Nata montada")).build());
                order5.getDetails().add(order5Detail1);
                order5.getDetails().add(order5Detail2);
                orderRepository.save(order5);

                // Cupones generales de demostración
                couponRepository.save(Coupon.builder().code("TERRA10")
                                .description("10 % de descuento en tu pedido")
                                .discountType(DiscountType.PERCENTAGE).discountValue(10.0)
                                .maxDiscount(8000.0).minOrderAmount(20000.0).active(true).build());
                couponRepository.save(Coupon.builder().code("DOMI5000")
                                .description("$5.000 de descuento")
                                .discountType(DiscountType.FIXED_AMOUNT).discountValue(5000.0)
                                .active(true).build());
                // Cupones personales de demostración para el cliente 1 (Juan)
                couponRepository.save(Coupon.builder().code("BIENVENIDA-" + client1.getId())
                                .description("Descuento de bienvenida")
                                .discountType(DiscountType.PERCENTAGE).discountValue(10.0)
                                .expiresAt(now.plusDays(30)).maxUses(1).active(true)
                                .client(client1).build());
                couponRepository.save(Coupon.builder().code("JUAN15K")
                                .description("$15.000 de descuento para ti")
                                .discountType(DiscountType.FIXED_AMOUNT).discountValue(15000.0)
                                .minOrderAmount(50000.0).expiresAt(now.plusDays(10)).active(true)
                                .client(client1).build());
                // Ya vencido: sirve para probar la sección "Expirados"
                couponRepository.save(Coupon.builder().code("VERANO15")
                                .description("15 % de descuento de temporada")
                                .discountType(DiscountType.PERCENTAGE).discountValue(15.0)
                                .startsAt(now.minusDays(40)).expiresAt(now.minusDays(5)).active(true)
                                .client(client1).build());

                // Anuncios de demostración para el home
                announcementRepository.save(Announcement.builder()
                                .title("¡Bienvenidos a Terra Galega!")
                                .description("Descubre nuestra carta de platos tradicionales y disfruta de la auténtica gastronomía gallega.")
                                .imageUrl("https://png.pngtree.com/thumb_back/fh260/background/20240425/pngtree-top-desk-with-blur-restaurant-background-wooden-table-image_15665383.jpg")
                                .build());

                // Reseñas de demostración: una por cliente (relación 1 a 1), todas buenas
                // (4 o 5 estrellas) para que el carrusel de opiniones tenga movimiento.
                String[][] reviews = {
                                { "juan.garcia@gmail.com", "5",
                                                "Auténtica cocina gallega. El pulpo a feira llegó perfecto y el servicio fue impecable." },
                                { "marta.souto@outlook.com", "5",
                                                "Celebramos un aniversario en el balcón y la atención fue de diez. El maridaje con albariño, un acierto." },
                                { "diego.pardo@gmail.com", "5",
                                                "La mejor fabada que he probado en Bogotá. Reservar por la web fue rapidísimo." },
                                { "maria.lopez@hotmail.com", "5",
                                                "El pulpo a la brasa es extraordinario. El servicio fue cálido y todo llegó en su punto." },
                                { "carlos.rodriguez@gmail.com", "5",
                                                "La fideuá y el cachopo estaban espectaculares. Ingredientes de primera y mucho sabor en cada plato." },
                                { "andres.martinez@outlook.com", "4",
                                                "Muy buen ambiente en el salón principal. Las croquetas de jamón, de las mejores de la ciudad." },
                                { "laura.ramirez@gmail.com", "5",
                                                "Hicimos la cena de grado en la sala de celebración y todo salió perfecto. Volveremos." },
                                { "sebastian.torres@yahoo.com", "5",
                                                "El menú de maridaje vale cada peso: cada vino estaba pensado para el plato." },
                                { "camila.moreno@gmail.com", "4",
                                                "Las croquetas y la tarta de queso vasca me encantaron. Volveré a pedir pronto." },
                                { "valeria.castro@gmail.com", "5",
                                                "Un restaurante elegante, con platos generosos y un equipo muy atento." },
                                { "felipe.gutierrez@outlook.com", "5",
                                                "El arroz negro y el vino de la casa, una combinación perfecta. Ambiente acogedor." },
                                { "natalia.rojas@hotmail.com", "5",
                                                "Pedimos a domicilio y llegó caliente y bien presentado. Se nota la calidad." },
                                { "juliana.vargas@gmail.com", "4",
                                                "La mesa de arco junto a la pared es muy cómoda para una cena tranquila. Muy recomendado." },
                                { "santiago.ospina@gmail.com", "5",
                                                "El chuletón a la piedra estaba en su punto exacto. Gran carta de vinos." },
                                { "daniel.quintero@yahoo.com", "5",
                                                "Celebramos un cumpleaños y nos sorprendieron con un postre. Detalles que enamoran." },
                                { "paula.herrera@icloud.com", "5",
                                                "Las vieiras gratinadas son una delicia. Precio alto pero lo vale." },
                                { "alejandro.navarro@gmail.com", "4",
                                                "Buen servicio y platos muy bien elaborados. La terraza del segundo piso es preciosa." },
                                { "gabriela.salazar@outlook.com", "5",
                                                "La tarta de Santiago es la mejor que he probado fuera de Galicia." },
                                { "ricardo.mendez@gmail.com", "5",
                                                "Fuimos por una cena de negocios y quedamos encantados con la atención y la discreción." },
                                { "lucia.pineda@hotmail.com", "5",
                                                "El maridaje de quesos gallegos con vino tinto fue una experiencia inolvidable." },
                                { "esteban.cardenas@gmail.com", "4",
                                                "Todo muy rico y bien servido. La reserva con anticipo da tranquilidad de que la mesa está lista." },
                                { "manuela.rios@gmail.com", "5",
                                                "El caldo gallego me recordó a la cocina de mi abuela. Volveré sin duda." },
                                { "hector.duarte@outlook.com", "5",
                                                "Excelente lugar para una cita: ambiente cálido, buena música y comida espectacular." },
                                { "adriana.beltran@yahoo.com", "5",
                                                "Los mariscos son fresquísimos. La parrillada para compartir es una maravilla." },
                                { "martin.acosta@gmail.com", "4",
                                                "Muy buena experiencia en la galería del segundo piso. El personal conoce bien la carta." },
                                { "carolina.suarez@hotmail.com", "5",
                                                "Pedí la empanada gallega y el lacón con grelos: sabores auténticos y porciones generosas." },
                                { "tomas.villamil@gmail.com", "5",
                                                "El sommelier nos recomendó un godello que combinó perfecto con el pescado. Gran servicio." },
                                { "diana.escobar@outlook.com", "5",
                                                "Celebramos la despedida de soltera en la sala de celebración. Todo impecable." },
                                { "samuel.pacheco@gmail.com", "4",
                                                "Platos deliciosos y un ambiente muy bonito. Ideal para ocasiones especiales." },
                                { "elena.barrios@icloud.com", "5",
                                                "La crema catalana y el café de pota, el cierre perfecto para una gran cena." },
                                { "miguel.forero@gmail.com", "5",
                                                "De los mejores restaurantes de la ciudad. Atención cercana y cocina de altura." },
                                { "sara.londono@outlook.com", "5",
                                                "Reservé para la revelación de género de mi bebé y el equipo lo hizo muy especial." },
                };
                for (int i = 0; i < reviews.length; i++) {
                        reviewRepository.save(Review.builder()
                                        .client(clientRepository.findByEmail(reviews[i][0]))
                                        .rating(Integer.parseInt(reviews[i][1]))
                                        .comment(reviews[i][2])
                                        .createdAt(now.minusDays(reviews.length - i).minusHours(i % 5))
                                        .build());
                }

                // Reservas reales ya pagadas (anticipo de 50.000). Ocupan sus mesas en esa
                // fecha
                // y hora, así el plano de reservas las muestra como "Reservada". Se reparten
                // entre hoy y los próximos días, varias en el turno de las 19:30 (el que abre
                // la página por defecto). juan.garcia@gmail.com (cliente de prueba) no tiene
                // reserva para poder probar el flujo completo con la pasarela.
                LocalDate today = LocalDate.now();
                Object[][] reservations = {
                                // correo, nombre de la reserva, mesa, días desde hoy, hora, sillas, temática
                                { "marta.souto@outlook.com", "Aniversario Souto", 201, 0, "19:30", 2,
                                                ReservationTheme.LOVE },
                                { "diego.pardo@gmail.com", "Cena con amigos", 118, 0, "19:30", 4,
                                                ReservationTheme.NONE },
                                { "maria.lopez@hotmail.com", "Cumpleaños de María", 127, 0, "19:30", 6,
                                                ReservationTheme.BIRTHDAY },
                                { "carlos.rodriguez@gmail.com", "Reunión de negocios", 112, 0, "19:30", 4,
                                                ReservationTheme.BUSINESS },
                                { "andres.martinez@outlook.com", "Maridaje gallego", 103, 0, "19:30", 2,
                                                ReservationTheme.PAIRING },
                                { "laura.ramirez@gmail.com", "Grado de Laura", 233, 0, "19:30", 16,
                                                ReservationTheme.GRADUATION },
                                { "sebastian.torres@yahoo.com", "Cena familiar", 225, 0, "19:30", 4,
                                                ReservationTheme.NONE },
                                { "camila.moreno@gmail.com", "Cita especial", 221, 0, "19:30", 2,
                                                ReservationTheme.LOVE },
                                { "valeria.castro@gmail.com", "Almuerzo de equipo", 120, 0, "21:00", 4,
                                                ReservationTheme.BUSINESS },
                                { "felipe.gutierrez@outlook.com", "Noche de vinos", 105, 0, "21:00", 2,
                                                ReservationTheme.PAIRING },
                                { "natalia.rojas@hotmail.com", "Cena de Natalia", 208, 0, "21:00", 4,
                                                ReservationTheme.NONE },
                                { "juliana.vargas@gmail.com", "Despedida de soltera", 234, 1, "19:30", 14,
                                                ReservationTheme.BACHELOR_PARTY },
                                { "santiago.ospina@gmail.com", "Cena de aniversario", 207, 1, "19:30", 2,
                                                ReservationTheme.LOVE },
                                { "daniel.quintero@yahoo.com", "Celebración familiar", 129, 1, "19:30", 6,
                                                ReservationTheme.BIRTHDAY },
                                { "paula.herrera@icloud.com", "Revelación de género", 233, 1, "13:30", 18,
                                                ReservationTheme.GENDER_REVEAL },
                                { "alejandro.navarro@gmail.com", "Almuerzo de negocios", 117, 1, "13:30", 4,
                                                ReservationTheme.BUSINESS },
                                { "gabriela.salazar@outlook.com", "Cena tranquila", 116, 1, "21:00", 3,
                                                ReservationTheme.NONE },
                                { "ricardo.mendez@gmail.com", "Maridaje con amigos", 228, 2, "19:30", 6,
                                                ReservationTheme.PAIRING },
                                { "lucia.pineda@hotmail.com", "Cumpleaños de Lucía", 230, 2, "19:30", 5,
                                                ReservationTheme.BIRTHDAY },
                                { "esteban.cardenas@gmail.com", "Cena de grado", 234, 2, "21:00", 12,
                                                ReservationTheme.GRADUATION },
                                { "manuela.rios@gmail.com", "Cita en el balcón", 204, 3, "19:30", 2,
                                                ReservationTheme.LOVE },
                                { "hector.duarte@outlook.com", "Comida de empresa", 119, 3, "13:30", 4,
                                                ReservationTheme.BUSINESS },
                                { "adriana.beltran@yahoo.com", "Almuerzo familiar", 109, 4, "12:00", 4,
                                                ReservationTheme.NONE },
                                { "martin.acosta@gmail.com", "Cena de amigos", 223, 5, "19:30", 4,
                                                ReservationTheme.NONE },
                };
                for (Object[] data : reservations) {
                        Client client = clientRepository.findByEmail((String) data[0]);
                        Reservation reservation = Reservation.builder()
                                        .client(client)
                                        .name((String) data[1])
                                        .table(diningTableRepository.findById((Integer) data[2]).orElseThrow())
                                        .date(today.plusDays((Integer) data[3]))
                                        .time(LocalTime.parse((String) data[4]))
                                        .seats((Integer) data[5])
                                        .theme((ReservationTheme) data[6])
                                        .paymentState(Reservation.PAID)
                                        .build();
                        client.setReservation(reservation);
                        reservationRepository.save(reservation);
                }
        }
}