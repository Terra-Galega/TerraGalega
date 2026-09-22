package com.example.demo;

import com.example.demo.controller.OrderController;
import org.springframework.stereotype.Component;

import com.example.demo.entities.*;
import com.example.demo.repository.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import java.util.List;
import java.util.Random;
import java.time.LocalDateTime;

import jakarta.transaction.Transactional;

@Component
public class DataLoader implements CommandLineRunner {

        private final OrderController orderController;

        @Autowired
        private ClientRepository clientRepository;

        @Autowired
        private AdminRepository adminRepository;

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private CategoryRepository categoryRepository;

        @Autowired
        private AddOnRepository addonRepository;

        @Autowired
        private DeliveryPersonRepository deliveryPersonRepository;

        @Autowired
        private OperatorRepository operatorRepository;

        @Autowired
        private OrderRepository orderRepository;

        DataLoader(OrderController orderController) {
                this.orderController = orderController;
        }

        // --- Helpers para reutilizar los productos/addOns ya cargados arriba,
        // en vez de duplicar su creación ---
        private Product findProductByName(String name) {
                return productRepository.findAll().stream()
                                .filter(p -> p.getName().equals(name))
                                .findFirst()
                                .orElseThrow();
        }

        private AddOn findAddOnByName(String name) {
                return addonRepository.findAll().stream()
                                .filter(a -> a.getName().equals(name))
                                .findFirst()
                                .orElseThrow();
        }

        @Override
        @Transactional
        public void run(String... args) throws Exception {
                Random random = new Random(42);

                clientRepository.save(
                                Client.builder().name("Juan").lastName("García").email("cliente@terra.com")
                                                .password("cliente123").phone("+57 300 000 0000")
                                                .role(Role.CLIENT).build());
                clientRepository.save(
                                Client.builder().name("Marta").lastName("Souto").email("marta.souto@terra.com")
                                                .password("marta123").phone("+57 310 555 1122")
                                                .role(Role.CLIENT).build());
                clientRepository.save(
                                Client.builder().name("Diego").lastName("Pardo").email("diego.pardo@terra.com")
                                                .password("diego123").phone("+57 320 444 9988")
                                                .role(Role.CLIENT).build());
                clientRepository.save(
                                Client.builder().name("María").lastName("López").email("maria.lopez@terra.com")
                                                .password("maria123").phone("+57 310 123 4567")
                                                .role(Role.CLIENT).build());
                clientRepository.save(
                                Client.builder().name("Carlos").lastName("Rodríguez")
                                                .email("carlos.rodriguez@terra.com")
                                                .password("carlos123").phone("+57 315 987 6543")
                                                .role(Role.CLIENT).build());
                clientRepository.save(
                                Client.builder().name("Andrés").lastName("Martínez").email("andres.martinez@terra.com")
                                                .password("andres123").phone("+57 301 222 3344")
                                                .role(Role.CLIENT).build());
                clientRepository.save(
                                Client.builder().name("Laura").lastName("Ramírez").email("laura.ramirez@terra.com")
                                                .password("laura123").phone("+57 311 666 7788")
                                                .role(Role.CLIENT).build());
                clientRepository.save(
                                Client.builder().name("Sebastián").lastName("Torres")
                                                .email("sebastian.torres@terra.com")
                                                .password("sebastian123").phone("+57 322 111 2233")
                                                .role(Role.CLIENT).build());
                clientRepository.save(
                                Client.builder().name("Camila").lastName("Moreno").email("camila.moreno@terra.com")
                                                .password("camila123").phone("+57 300 888 4455")
                                                .role(Role.CLIENT).build());
                clientRepository.save(
                                Client.builder().name("Felipe").lastName("Castro").email("felipe.castro@terra.com")
                                                .password("felipe123").phone("+57 316 333 5566")
                                                .role(Role.CLIENT).build());

                // admin
                adminRepository.save(
                                Admin.builder().name("Carlos").lastName("Martínez").email("admin@terra.com")
                                                .password("admin123").role(Role.ADMIN).build());
                adminRepository.save(
                                Admin.builder().name("Ana").lastName("Fernández").email("ana.fernandez@terra.com")
                                                .password("ana123").role(Role.ADMIN).build());
                adminRepository.save(
                                Admin.builder().name("Roberto").lastName("Silva").email("roberto.silva@terra.com")
                                                .password("roberto123").role(Role.ADMIN).build());
                adminRepository.save(
                                Admin.builder().name("Patricia").lastName("Gómez").email("patricia.gomez@terra.com")
                                                .password("patricia123").role(Role.ADMIN).build());
                adminRepository.save(
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

                
                Category starters = categoryRepository.findByName("Entradas");
                Category seafood = categoryRepository.findByName("Mariscos");
                Category meats = categoryRepository.findByName("Carnes");
                Category desserts = categoryRepository.findByName("Postres");

                // adicionales para entradas
                addonRepository.save(AddOn.builder().name("Porción de Pan Rústico")
                                .description("Pan gallego artesanal, ideal para acompañar tus entradas y mojar salsas.")
                                .price(3500.0).Active(true).category(starters).build());

                addonRepository.save(AddOn.builder().name("Extra Salsa Brava")
                                .description("Una porción extra de nuestra salsa brava ligeramente picante.")
                                .price(2500.0).Active(true).category(starters).build());

                addonRepository.save(AddOn.builder().name("Picos Camperos")
                                .description("Palitos de pan crujientes típicos, perfectos para picar.")
                                .price(2000.0).Active(true).category(starters).build());

                // adicionales para mariscos
                addonRepository.save(AddOn.builder().name("Ración de Cachelos")
                                .description("Patatas cocidas con pimentón, el acompañante perfecto para el pulpo.")
                                .price(6000.0).Active(true).category(seafood).build());

                addonRepository.save(AddOn.builder().name("Salsa de Limón y Perejil")
                                .description("Toque cítrico y fresco extra para tus mariscos y pescados.")
                                .price(3000.0).Active(true).category(seafood).build());

                addonRepository.save(AddOn.builder().name("Mayonesa Casera Suave")
                                .description("Nuestra mayonesa tradicional hecha en casa, sin ajo.")
                                .price(2500.0).Active(true).category(seafood).build());

                // adicionales para carnes
                addonRepository.save(AddOn.builder().name("Ensalada Verde de Guarnición")
                                .description("Fresca mezcla de lechugas para equilibrar tus carnes.")
                                .price(5000.0).Active(true).category(meats).build());

                addonRepository.save(AddOn.builder().name("Patatas Panaderas")
                                .description("Patatas horneadas lentamente a fuego lento, ideales para carnes asadas.")
                                .price(6000.0).Active(true).category(meats).build());

                addonRepository.save(AddOn.builder().name("Puré de Patatas Trufado")
                                .description("Cremoso puré de patata con un toque de aceite de trufa blanca.")
                                .price(6500.0).Active(true).category(meats).build());

                // adicionales para postres
                addonRepository.save(AddOn.builder().name("Bola de Helado de Vainilla")
                                .description("Combina perfecto con torrijas, tartas o postres calientes.")
                                .price(4500.0).Active(true).category(desserts).build());

                addonRepository.save(AddOn.builder().name("Nata Montada Extra")
                                .description("Nata fresca montada al momento, ideal para acompañar dulces.")
                                .price(3000.0).Active(true).category(desserts).build());



                // Products

                productRepository.save(
                                Product.builder().name("Tortilla Española")
                                                .description("Tortilla tradicional de patatas y cebolla, jugosa por dentro y dorada por fuera.")
                                                .price(18000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://images.cookforyourlife.org/wp-content/uploads/2018/05/Tortilla-Espanola-1-696x464.jpg")
                                                .active(true).popular(false).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Patatas Bravas")
                                                .description("Patatas crujientes acompañadas de salsa brava casera ligeramente picante.")
                                                .price(16000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://www.deliciousmagazine.co.uk/wp-content/uploads/2018/09/patatas-bravas.jpg")
                                                .active(true).popular(false).vegetarian(true).spicyMild(true)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Gambas al Ajillo")
                                                .description("Gambas salteadas en aceite de oliva con ajo, guindilla y perejil fresco.")
                                                .price(32000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://invitadoinvierno.com/wp-content/uploads/2019/07/receta-gambas-ajillo-0.jpg")
                                                .active(true).popular(true).vegetarian(false).spicyMild(true)
                                                .spicyHot(true).containsNuts(false).containsSeafood(true)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Calamares a la Romana")
                                                .description("Anillas de calamar rebozadas y fritas hasta quedar doradas y crujientes.")
                                                .price(28000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://newluxbrand.com/recetas/wp-content/uploads/2022/02/calamares-a-la-romana-Newlux.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(true)
                                                .containsGluten(true).build());

                productRepository.save(
                                Product.builder().name("Pulpo a la Brasa")
                                                .description("Tentáculo de pulpo a la brasa acompañado de patatas confitadas y aceite de oliva.")
                                                .price(42000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://esenciadelmar.es/wp-content/uploads/2023/08/pulpo-brasa-parilla-1200x700.jpeg")
                                                .active(true).popular(true).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(true)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Arroz Negro")
                                                .description("Arroz meloso cocinado con tinta de calamar, sepia, gambas y alioli casero.")
                                                .price(48000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://assets.tmecosys.com/image/upload/t_web_rdp_recipe_584x480/img/recipe/ras/Assets/f7f1b6c8-1a85-435e-b136-1e4cfa082903/Derivates/fbb52d55-2deb-4480-ac79-08b3522e4b09.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(true)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Fideuá Valenciana")
                                                .description("Fideos tostados cocinados con caldo de pescado, gambas, calamares y alioli.")
                                                .price(52000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://i0.wp.com/spainonafork.com/wp-content/uploads/2018/10/fideua3-11.png?fit=750%2C750&ssl=1")
                                                .active(true).popular(true).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(true)
                                                .containsGluten(true).build());

                productRepository.save(
                                Product.builder().name("Bacalao al Pil Pil")
                                                .description("Lomo de bacalao confitado en aceite de oliva con emulsión de ajo y guindilla.")
                                                .price(46000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://www.aceitesdeolivadeespana.com/wp-content/uploads/2020/09/bacalao-al-pil-pil.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(true)
                                                .spicyHot(false).containsNuts(false).containsSeafood(true)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Merluza a la Vasca")
                                                .description("Merluza fresca cocinada con salsa verde, almejas, espárragos y perejil.")
                                                .price(44000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRDV3yWMquqJyKwvVQG3hdrJ445tIeQrDw-3qUNt6Pox0VNHIWKldCADtz1&s=10")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(true)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Dorada al Horno")
                                                .description("Dorada fresca al horno con patatas, cebolla, limón y aceite de oliva virgen extra.")
                                                .price(39000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://lacocinadefrabisa.lavozdegalicia.es/wp-content/uploads/2016/03/dorada.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(true)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Cochinillo Segoviano")
                                                .description("Cochinillo asado lentamente hasta conseguir una piel crujiente y una carne tierna.")
                                                .price(68000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://www.tabladillo.es/wp-content/uploads/2024/07/Cochinillo-Tabladillo-Segovia-scaled.jpg")
                                                .active(true).popular(true).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Cordero Asado")
                                                .description("Pierna de cordero asada lentamente con hierbas aromáticas y patatas panaderas.")
                                                .price(62000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://assets.tmecosys.com/image/upload/t_web_rdp_recipe_584x480/img/recipe/ras/Assets/1C6B162D-43E7-4A9B-8690-3F873A07ED29/Derivates/D494B6BC-3836-410B-8455-B4555AAA565C.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Carrilleras de Cerdo")
                                                .description("Carrilleras de cerdo cocinadas a fuego lento en salsa de vino tinto y verduras.")
                                                .price(48000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://recetasdecocina.elmundo.es/wp-content/uploads/2025/12/carrilleras-de-cerdo-al-vino-tinto-1024x683.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Chorizo a la Sidra")
                                                .description("Chorizo asturiano cocinado lentamente en sidra natural hasta quedar jugoso y aromático.")
                                                .price(22000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://www.carolinescooking.com/wp-content/uploads/2023/06/chorizo-in-cider-featured-pic-sq.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Fabada Asturiana")
                                                .description("Guiso tradicional de fabes asturianas con chorizo, morcilla y panceta.")
                                                .price(38000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://www.justspices.es/media/recipe/Fabada-asturiana.webp")
                                                .active(true).popular(true).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Callos a la Madrileña")
                                                .description("Guiso tradicional de callos de ternera con chorizo, morcilla y especias.")
                                                .price(36000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://static.bainet.es/clip/9932fc94-4a07-48d9-b06d-eb170c7f222c_source-aspect-ratio_1600w_0.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Rabo de Toro")
                                                .description("Estofado de rabo de toro cocinado lentamente con vino tinto, verduras y especias.")
                                                .price(58000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://www.foodandwine.com/thmb/1qa4CivgyDuYGgHVAM_0GYXkQgw=/1500x0/filters:no_upscale():max_bytes(150000):strip_icc()/rabo-de-toro-XL-RECIPE0917-5f241cba48de485b935f0e882b504fe5.jpg")
                                                .active(true).popular(true).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Pollo al Ajillo")
                                                .description("Pollo dorado en aceite de oliva con abundante ajo, vino blanco y perejil.")
                                                .price(32000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://cookingtheglobe.com/wp-content/uploads/2016/03/pollo-al-ajillo-garlic-chicken-recipe-2.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Secreto Ibérico")
                                                .description("Corte de cerdo ibérico a la parrilla servido con patatas y reducción de vino tinto.")
                                                .price(55000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://assets.tmecosys.com/image/upload/t_web_rdp_recipe_584x480/img/recipe/ras/Assets/58BEF3B8-6B5A-452E-9A4D-CEB1E38098A6/Derivates/c1a17709-3a16-4fa1-8c71-937c04747466.jpg")
                                                .active(true).popular(true).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Migas Extremeñas")
                                                .description("Migas de pan salteadas con ajo, pimentón, chorizo y panceta al estilo extremeño.")
                                                .price(28000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://recetasdecocina.elmundo.es/wp-content/uploads/2024/11/migas-extremenas-receta-1024x683.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(true).build());

                productRepository.save(
                                Product.builder().name("Gazpacho Andaluz")
                                                .description("Sopa fría tradicional de tomate, pepino, pimiento, ajo y aceite de oliva.")
                                                .price(15000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://es-mycooktouch.group-taurus.com/image/recipe/540x391/gazpacho-andaluz")
                                                .active(true).popular(false).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Salmorejo Cordobés")
                                                .description("Crema fría de tomate y pan acompañada de huevo cocido y jamón serrano.")
                                                .price(17000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://www.aceitesdeolivadeespana.com/wp-content/uploads/2024/04/salmorejo_jamon_iberico.jpeg")
                                                .active(true).popular(true).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(true).build());

                productRepository.save(
                                Product.builder().name("Pimientos del Piquillo Rellenos")
                                                .description("Pimientos del piquillo rellenos de bacalao y cubiertos con salsa de pimientos.")
                                                .price(26000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTq0RRc3EIfGd43qP4eJR-7bikx3GGdDG02z0DZkU4mt9U1gMqEQRFKZ9u7&s=10")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(true)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Berenjenas con Miel")
                                                .description("Berenjenas crujientes acompañadas de miel de caña y una pizca de sal marina.")
                                                .price(19000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQq2dnL2PshnrGDZfPPIZG-9l5EhRmdsfVZkBUUmKoE3w&s=10")
                                                .active(true).popular(false).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Ensaladilla Rusa")
                                                .description("Ensaladilla tradicional de patata, zanahoria, guisantes, huevo y atún con mayonesa casera.")
                                                .price(18000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://www.seriouseats.com/thmb/Qj2Ta8m0V7eHoqB0Z6Z1eFD3D-I=/1500x0/filters:no_upscale():max_bytes(150000):strip_icc()/20260515-SEA-RussianSalad-AmandaSuarez-15-7f6b215dd4434cf7aec5f3095cd09ed1.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(true)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Boquerones en Vinagre")
                                                .description("Boquerones marinados en vinagre con ajo, perejil y aceite de oliva virgen extra.")
                                                .price(21000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://img.turkishstylecooking.com/wp-content/uploads/2025/01/boquerones_en_vinagre1.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(true)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Pisto Manchego")
                                                .description("Guiso de tomate, calabacín, pimiento, cebolla y berenjena con aceite de oliva.")
                                                .price(22000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://www.aceitesdeolivadeespana.com/wp-content/uploads/2021/02/pisto-manchego-receta.jpg")
                                                .active(true).popular(false).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Huevos Rotos con Jamón")
                                                .description("Huevos fritos sobre patatas caseras acompañados de láminas de jamón ibérico.")
                                                .price(29000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSol8_W3dxVbXfF6nucJj4zoDIE2LXJwddqEjHa8WdfQw&s=10")
                                                .active(true).popular(true).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Bocadillo de Calamares")
                                                .description("Tradicional bocadillo madrileño de calamares fritos servido con alioli casero.")
                                                .price(23000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://www.tiaalia.com/wp-content/uploads/2017/11/bocadillo-de-calamares.jpg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(true)
                                                .containsGluten(true).build());

                productRepository.save(
                                Product.builder().name("Cachopo Asturiano")
                                                .description("Filetes de ternera rellenos de jamón serrano y queso, empanados y fritos hasta quedar dorados.")
                                                .price(52000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://tablasdelcampillin.com/wp-content/uploads/2023/05/cachopos-capsa_4aH-Editar.jpg")
                                                .active(true).popular(true).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(true).build());

                productRepository.save(
                                Product.builder().name("Crema Catalana")
                                                .description("Postre tradicional catalán de crema suave con una fina capa de azúcar caramelizado.")
                                                .price(16000.0)
                                                .category(categoryRepository.findByName("Postres"))
                                                .imageUrl("https://assets.tmecosys.com/image/upload/t_web_rdp_recipe_584x480/img/recipe/ras/Assets/2d814c07a2145d881d1e701d4b27c029/Derivates/93949c61852021496362279d132f6996ec0ac30b.jpg")
                                                .active(true).popular(false).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Arroz con Leche")
                                                .description("Postre cremoso de arroz cocido lentamente con leche, canela y cáscara de limón.")
                                                .price(14000.0)
                                                .category(categoryRepository.findByName("Postres"))
                                                .imageUrl("https://recetasdecocina.elmundo.es/wp-content/uploads/2024/11/arroz-con-leche-1024x683.jpg")
                                                .active(true).popular(false).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Churros con Chocolate")
                                                .description("Churros españoles crujientes acompañados de una taza de chocolate caliente y espeso.")
                                                .price(18000.0)
                                                .category(categoryRepository.findByName("Postres"))
                                                .imageUrl("https://www.realsimple.com/thmb/74r1RaSWyOqGyLD9vakiGkmyIYQ=/1500x0/filters:no_upscale():max_bytes(150000):strip_icc()/churros-chocolate-0f3587a6a59f4696af610cafe100d1fe.jpg")
                                                .active(true).popular(true).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(true).build());

                productRepository.save(
                                Product.builder().name("Torrijas")
                                                .description("Rebanadas de pan empapadas en leche aromatizada con canela y limón, doradas y espolvoreadas con azúcar.")
                                                .price(15000.0)
                                                .category(categoryRepository.findByName("Postres"))
                                                .imageUrl("https://images.aws.nestle.recipes/resized/2024_10_28T12_32_34_badun_images.badun.es_dd1722b7f5d9_torrija_con_helado_de_torrija_y_coulis_de_frutos_rojos_1290_742.jpg")
                                                .active(true).popular(false).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(true).build());

                productRepository.save(
                                Product.builder().name("Tarta de Queso Vasca")
                                                .description("Tarta de queso cremosa de interior suave y superficie caramelizada al estilo vasco.")
                                                .price(19000.0)
                                                .category(categoryRepository.findByName("Postres"))
                                                .imageUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSTPsxvBLFVs3SLnnc7A8F7_spaek91-Ghw1M3E8vb0ug&s=10")
                                                .active(true).popular(true).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Flan de Huevo")
                                                .description("Flan casero de huevo y leche servido con caramelo líquido y una textura delicadamente cremosa.")
                                                .price(14000.0)
                                                .category(categoryRepository.findByName("Postres"))
                                                .imageUrl("https://static.bainet.es/clip/9c993f21-16d2-4148-bece-ba68ae1e172a_source-aspect-ratio_1600w_0.jpg")
                                                .active(true).popular(false).vegetarian(true).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Sangría Española")
                                                .description("Bebida tradicional española preparada con vino tinto, frutas frescas y un toque cítrico.")
                                                .price(18000.0)
                                                .category(categoryRepository.findByName("Bebidas"))
                                                .imageUrl("https://descorcha.com/cdn/shop/articles/17380724881149_089e6f94-47d6-4852-b73d-a147c7542d85.jpg?v=1765379043")
                                                .active(true).popular(true).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Horchata de Chufa")
                                                .description("Bebida tradicional valenciana elaborada a base de chufa, fresca y ligeramente dulce.")
                                                .price(12000.0)
                                                .category(categoryRepository.findByName("Bebidas"))
                                                .imageUrl("https://www.finedininglovers.es/sites/default/files/styles/1_1_768x768/public/recipe_content_images/horchata-de-chufa%C2%A9iStock.jpg.webp?itok=5pehvI1m")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Tinto de Verano")
                                                .description("Bebida refrescante preparada con vino tinto, gaseosa y un toque de limón.")
                                                .price(14000.0)
                                                .category(categoryRepository.findByName("Bebidas"))
                                                .imageUrl("https://www.pequerecetas.com/wp-content/uploads/2022/06/como-hacer-tinto-de-verano.jpeg")
                                                .active(true).popular(false).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                productRepository.save(
                                Product.builder().name("Café Cortado")
                                                .description("Café espresso servido con una pequeña cantidad de leche caliente.")
                                                .price(8000.0)
                                                .category(categoryRepository.findByName("Bebidas"))
                                                .imageUrl("https://blogdelcafe.com/wp-content/uploads/2025/08/cortado-1024x647.webp")
                                                .active(true).popular(true).vegetarian(false).spicyMild(false)
                                                .spicyHot(false).containsNuts(false).containsSeafood(false)
                                                .containsGluten(false).build());

                // DeliveryPersons
                DeliveryPerson deliveryPerson1 = deliveryPersonRepository.save(
                                DeliveryPerson.builder().name("Pedro").lastName("Sánchez")
                                                .email("pedro.sanchez@terra.com").password("pedro123")
                                                .role(Role.DELIVERY_PERSON).identification("CC1001234567")
                                                .phone("+57 301 500 1001").available(true).active(true).build());
                DeliveryPerson deliveryPerson2 = deliveryPersonRepository.save(
                                DeliveryPerson.builder().name("Luisa").lastName("Fernández")
                                                .email("luisa.fernandez@terra.com").password("luisa123")
                                                .role(Role.DELIVERY_PERSON).identification("CC1001234568")
                                                .phone("+57 301 500 1002").available(true).active(true).build());
                deliveryPersonRepository.save(
                                DeliveryPerson.builder().name("Miguel Ángel").lastName("Ruiz")
                                                .email("miguel.ruiz@terra.com").password("miguel123")
                                                .role(Role.DELIVERY_PERSON).identification("CC1001234569")
                                                .phone("+57 301 500 1003").available(false).active(true).build());
                deliveryPersonRepository.save(
                                DeliveryPerson.builder().name("Daniela").lastName("Vargas")
                                                .email("daniela.vargas@terra.com").password("daniela123")
                                                .role(Role.DELIVERY_PERSON).identification("CC1001234570")
                                                .phone("+57 301 500 1004").available(true).active(true).build());
                deliveryPersonRepository.save(
                                DeliveryPerson.builder().name("Tomás").lastName("Herrera")
                                                .email("tomas.herrera@terra.com").password("tomas123")
                                                .role(Role.DELIVERY_PERSON).identification("CC1001234571")
                                                .phone("+57 301 500 1005").available(true).active(false).build());

                // Operators
                Operator operator1 = operatorRepository.save(
                                Operator.builder().name("Sofía").lastName("Jiménez")
                                                .email("sofia.jimenez@terra.com").password("sofia123")
                                                .role(Role.OPERATOR).username("sofiaj").active(true).build());
                Operator operator2 = operatorRepository.save(
                                Operator.builder().name("Mateo").lastName("Restrepo")
                                                .email("mateo.restrepo@terra.com").password("mateo123")
                                                .role(Role.OPERATOR).username("mateor").active(true).build());
                Operator operator3 = operatorRepository.save(
                                Operator.builder().name("Valentina").lastName("Cruz")
                                                .email("valentina.cruz@terra.com").password("valentina123")
                                                .role(Role.OPERATOR).username("valentinac").active(true).build());
                Operator operator4 = operatorRepository.save(
                                Operator.builder().name("Nicolás").lastName("Peña")
                                                .email("nicolas.pena@terra.com").password("nicolas123")
                                                .role(Role.OPERATOR).username("nicolasp").active(true).build());
                Operator operator5 = operatorRepository.save(
                                Operator.builder().name("Isabella").lastName("Mendoza")
                                                .email("isabella.mendoza@terra.com").password("isabella123")
                                                .role(Role.OPERATOR).username("isabellam").active(false).build());

                // Orders (usando los clientes, productos, addOns, operadores y
                // domiciliarios ya existentes)
                Client client1 = clientRepository.findByEmail("cliente@terra.com");
                Client client2 = clientRepository.findByEmail("marta.souto@terra.com");
                Client client3 = clientRepository.findByEmail("diego.pardo@terra.com");
                Client client4 = clientRepository.findByEmail("maria.lopez@terra.com");
                Client client5 = clientRepository.findByEmail("carlos.rodriguez@terra.com");

                // Pedido 1 - entregado
                Product tortilla = findProductByName("Tortilla Española");
                Product gambas = findProductByName("Gambas al Ajillo");
                Order order1 = Order.builder()
                                .client(client1)
                                .status(OrderStatus.DELIVERED)
                                .createdAt(LocalDateTime.of(2026, 1, 5, 12, 30))
                                .deliveredAt(LocalDateTime.of(2026, 1, 5, 13, 15))
                                .operator(operator1)
                                .deliveryPerson(deliveryPerson1)
                                .address("Calle 10 #5-20, Bogotá")
                                .build();
                OrderDetail order1Detail1 = OrderDetail.builder()
                                .order(order1).product(tortilla).quantity(2)
                                .unitPrice(tortilla.getPrice()).build();
                OrderDetail order1Detail2 = OrderDetail.builder()
                                .order(order1).product(gambas).quantity(1)
                                .unitPrice(gambas.getPrice()).build();
                order1Detail2.getOrderDetailAddOns().add(
                                OrderDetailAddOn.builder().orderDetail(order1Detail2)
                                                .addOn(findAddOnByName("Extra ajo")).build());
                order1.getDetails().add(order1Detail1);
                order1.getDetails().add(order1Detail2);
                orderRepository.save(order1);

                // Pedido 2 - en camino
                Product pulpo = findProductByName("Pulpo a la Brasa");
                Product sangria = findProductByName("Sangría Española");
                Order order2 = Order.builder()
                                .client(client2)
                                .status(OrderStatus.ON_THE_WAY)
                                .createdAt(LocalDateTime.of(2026, 1, 6, 18, 0))
                                .operator(operator2)
                                .deliveryPerson(deliveryPerson2)
                                .address("Carrera 15 #45-10, Bogotá")
                                .build();
                OrderDetail order2Detail1 = OrderDetail.builder()
                                .order(order2).product(pulpo).quantity(1)
                                .unitPrice(pulpo.getPrice()).build();
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
                Product cachopo = findProductByName("Cachopo Asturiano");
                Product cremaCatalana = findProductByName("Crema Catalana");
                Order order3 = Order.builder()
                                .client(client3)
                                .status(OrderStatus.PREPARING)
                                .createdAt(LocalDateTime.of(2026, 1, 7, 19, 20))
                                .operator(operator3)
                                .address("Avenida 68 #20-33, Bogotá")
                                .build();
                OrderDetail order3Detail1 = OrderDetail.builder()
                                .order(order3).product(cachopo).quantity(1)
                                .unitPrice(cachopo.getPrice()).build();
                OrderDetail order3Detail2 = OrderDetail.builder()
                                .order(order3).product(cremaCatalana).quantity(2)
                                .unitPrice(cremaCatalana.getPrice()).build();
                order3Detail1.getOrderDetailAddOns().add(
                                OrderDetailAddOn.builder().orderDetail(order3Detail1)
                                                .addOn(findAddOnByName("Pan de millo")).build());
                order3.getDetails().add(order3Detail1);
                order3.getDetails().add(order3Detail2);
                orderRepository.save(order3);

                // Pedido 4 - confirmado (sin domiciliario asignado aún)
                Product patatasBravas = findProductByName("Patatas Bravas");
                Product cafeCortado = findProductByName("Café Cortado");
                Order order4 = Order.builder()
                                .client(client4)
                                .status(OrderStatus.CONFIRMED)
                                .createdAt(LocalDateTime.of(2026, 1, 8, 13, 5))
                                .operator(operator4)
                                .address("Calle 72 #10-34, Bogotá")
                                .build();
                OrderDetail order4Detail1 = OrderDetail.builder()
                                .order(order4).product(patatasBravas).quantity(3)
                                .unitPrice(patatasBravas.getPrice()).build();
                OrderDetail order4Detail2 = OrderDetail.builder()
                                .order(order4).product(cafeCortado).quantity(2)
                                .unitPrice(cafeCortado.getPrice()).build();
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
                                .createdAt(LocalDateTime.of(2026, 1, 9, 20, 45))
                                .operator(operator5)
                                .address("Transversal 9 #100-2, Bogotá")
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
        }
}