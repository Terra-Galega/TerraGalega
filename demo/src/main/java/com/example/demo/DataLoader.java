package com.example.demo;

import org.springframework.stereotype.Component;

import com.example.demo.entities.*;
import com.example.demo.repository.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import java.util.Random;

import jakarta.transaction.Transactional;

@Component
@Transactional
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
        private AddOnRepository addonRepository;

        @Override
        public void run(String... args) throws Exception {
                Random random = new Random(42);

                clientRepository.save(
                                Client.builder().name("Juan").lastName("García").email("cliente@terra.com")
                                                .password("cliente123").phone("+57 300 000 0000").build());
                clientRepository.save(
                                Client.builder().name("Marta").lastName("Souto").email("marta.souto@terra.com")
                                                .password("marta123").phone("+57 310 555 1122").build());
                clientRepository.save(
                                Client.builder().name("Diego").lastName("Pardo").email("diego.pardo@terra.com")
                                                .password("diego123").phone("+57 320 444 9988").build());
                clientRepository.save(
                                Client.builder().name("María").lastName("López").email("maria.lopez@terra.com")
                                                .password("maria123").phone("+57 310 123 4567").build());
                clientRepository.save(
                                Client.builder().name("Carlos").lastName("Rodríguez")
                                                .email("carlos.rodriguez@terra.com")
                                                .password("carlos123").phone("+57 315 987 6543").build());
                clientRepository.save(
                                Client.builder().name("Andrés").lastName("Martínez").email("andres.martinez@terra.com")
                                                .password("andres123").phone("+57 301 222 3344").build());
                clientRepository.save(
                                Client.builder().name("Laura").lastName("Ramírez").email("laura.ramirez@terra.com")
                                                .password("laura123").phone("+57 311 666 7788").build());
                clientRepository.save(
                                Client.builder().name("Sebastián").lastName("Torres")
                                                .email("sebastian.torres@terra.com")
                                                .password("sebastian123").phone("+57 322 111 2233").build());
                clientRepository.save(
                                Client.builder().name("Camila").lastName("Moreno").email("camila.moreno@terra.com")
                                                .password("camila123").phone("+57 300 888 4455").build());
                clientRepository.save(
                                Client.builder().name("Felipe").lastName("Castro").email("felipe.castro@terra.com")
                                                .password("felipe123").phone("+57 316 333 5566").build());

                // admin
                adminRepository.save(
                                Admin.builder().name("Carlos").lastName("Martínez").email("admin@terra.com")
                                                .password("admin123").build());

                // AddOns
                addonRepository.save(AddOn.builder().name("Ensalada verde")
                                .description("Adicional para Empanada").price(5000.0).Active(true).build());
                addonRepository.save(AddOn.builder().name("Extra ajo")
                                .description("Adicional para mariscos").price(2000.0).Active(true).build());
                addonRepository.save(AddOn.builder().name("Salsa de limón")
                                .description("Adicional para mariscos").price(3000.0).Active(true).build());

                addonRepository.save(AddOn.builder().name("Pan de millo")
                                .description("Adicional para Lacón").price(3500.0).Active(true).build());
                addonRepository.save(AddOn.builder().name("Cachelos")
                                .description("Adicional para Lacón").price(6000.0).Active(true).build());

                addonRepository.save(AddOn.builder().name("Nata montada")
                                .description("Adicional para Tarta de Santiago").price(3000.0).Active(true).build());
                addonRepository.save(AddOn.builder().name("Helado de vainilla")
                                .description("Adicional para Tarta de Santiago").price(4000.0).Active(true).build());

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

                int addOnCuantity = (int) addonRepository.findAll().size();

                for (Category c : categoryRepository.findAll()) {

                        int randomNum = random.nextInt(1, addOnCuantity + 1);

                        AddOn randomAddOn = addonRepository
                                        .findById(randomNum)
                                        .orElseThrow();

                        randomAddOn.setCategory(c);

                        addonRepository.save(randomAddOn);
                }

                // Products

                productRepository.save(
                                Product.builder().name("Tortilla Española")
                                                .description("Tortilla tradicional de patatas y cebolla, jugosa por dentro y dorada por fuera.")
                                                .price(18000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://images.unsplash.com/photo-1601050690597-df0568f70950?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Patatas Bravas")
                                                .description("Patatas crujientes acompañadas de salsa brava casera ligeramente picante.")
                                                .price(16000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://images.unsplash.com/photo-1598679253544-2c97992403ea?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Gambas al Ajillo")
                                                .description("Gambas salteadas en aceite de oliva con ajo, guindilla y perejil fresco.")
                                                .price(32000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://images.unsplash.com/photo-1559339352-11d035aa65de?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(true).build());

                productRepository.save(
                                Product.builder().name("Calamares a la Romana")
                                                .description("Anillas de calamar rebozadas y fritas hasta quedar doradas y crujientes.")
                                                .price(28000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Pulpo a la Brasa")
                                                .description("Tentáculo de pulpo a la brasa acompañado de patatas confitadas y aceite de oliva.")
                                                .price(42000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://images.unsplash.com/photo-1534080564583-6be75777b70a?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(true).build());

                productRepository.save(
                                Product.builder().name("Arroz Negro")
                                                .description("Arroz meloso cocinado con tinta de calamar, sepia, gambas y alioli casero.")
                                                .price(48000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Fideuá Valenciana")
                                                .description("Fideos tostados cocinados con caldo de pescado, gambas, calamares y alioli.")
                                                .price(52000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://images.unsplash.com/photo-1534080564583-6be75777b70a?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(true).build());

                productRepository.save(
                                Product.builder().name("Bacalao al Pil Pil")
                                                .description("Lomo de bacalao confitado en aceite de oliva con emulsión de ajo y guindilla.")
                                                .price(46000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Merluza a la Vasca")
                                                .description("Merluza fresca cocinada con salsa verde, almejas, espárragos y perejil.")
                                                .price(44000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://images.unsplash.com/photo-1544943910-4c1dc44aab44?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Dorada al Horno")
                                                .description("Dorada fresca al horno con patatas, cebolla, limón y aceite de oliva virgen extra.")
                                                .price(39000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Cochinillo Segoviano")
                                                .description("Cochinillo asado lentamente hasta conseguir una piel crujiente y una carne tierna.")
                                                .price(68000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://images.unsplash.com/photo-1544025162-d76694265947?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(true).build());

                productRepository.save(
                                Product.builder().name("Cordero Asado")
                                                .description("Pierna de cordero asada lentamente con hierbas aromáticas y patatas panaderas.")
                                                .price(62000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://images.unsplash.com/photo-1544025162-d76694265947?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Carrilleras de Cerdo")
                                                .description("Carrilleras de cerdo cocinadas a fuego lento en salsa de vino tinto y verduras.")
                                                .price(48000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://images.unsplash.com/photo-1544025162-d76694265947?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Chorizo a la Sidra")
                                                .description("Chorizo asturiano cocinado lentamente en sidra natural hasta quedar jugoso y aromático.")
                                                .price(22000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://images.unsplash.com/photo-1529692236671-f1f6cf9683ba?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Fabada Asturiana")
                                                .description("Guiso tradicional de fabes asturianas con chorizo, morcilla y panceta.")
                                                .price(38000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://images.unsplash.com/photo-1547592180-85f173990554?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(true).build());

                productRepository.save(
                                Product.builder().name("Callos a la Madrileña")
                                                .description("Guiso tradicional de callos de ternera con chorizo, morcilla y especias.")
                                                .price(36000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://images.unsplash.com/photo-1547592180-85f173990554?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Rabo de Toro")
                                                .description("Estofado de rabo de toro cocinado lentamente con vino tinto, verduras y especias.")
                                                .price(58000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://images.unsplash.com/photo-1544025162-d76694265947?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(true).build());

                productRepository.save(
                                Product.builder().name("Pollo al Ajillo")
                                                .description("Pollo dorado en aceite de oliva con abundante ajo, vino blanco y perejil.")
                                                .price(32000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://images.unsplash.com/photo-1532550907401-a500c9a57435?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Secreto Ibérico")
                                                .description("Corte de cerdo ibérico a la parrilla servido con patatas y reducción de vino tinto.")
                                                .price(55000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://images.unsplash.com/photo-1544025162-d76694265947?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(true).build());

                productRepository.save(
                                Product.builder().name("Migas Extremeñas")
                                                .description("Migas de pan salteadas con ajo, pimentón, chorizo y panceta al estilo extremeño.")
                                                .price(28000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://images.unsplash.com/photo-1547592180-85f173990554?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Gazpacho Andaluz")
                                                .description("Sopa fría tradicional de tomate, pepino, pimiento, ajo y aceite de oliva.")
                                                .price(15000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://images.unsplash.com/photo-1599021456807-25db0f974333?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Salmorejo Cordobés")
                                                .description("Crema fría de tomate y pan acompañada de huevo cocido y jamón serrano.")
                                                .price(17000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://images.unsplash.com/photo-1599021456807-25db0f974333?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(true).build());

                productRepository.save(
                                Product.builder().name("Pimientos del Piquillo Rellenos")
                                                .description("Pimientos del piquillo rellenos de bacalao y cubiertos con salsa de pimientos.")
                                                .price(26000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://images.unsplash.com/photo-1547592180-85f173990554?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Berenjenas con Miel")
                                                .description("Berenjenas crujientes acompañadas de miel de caña y una pizca de sal marina.")
                                                .price(19000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://images.unsplash.com/photo-1547592180-85f173990554?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Ensaladilla Rusa")
                                                .description("Ensaladilla tradicional de patata, zanahoria, guisantes, huevo y atún con mayonesa casera.")
                                                .price(18000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://images.unsplash.com/photo-1540420773420-3366772f4999?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Boquerones en Vinagre")
                                                .description("Boquerones marinados en vinagre con ajo, perejil y aceite de oliva virgen extra.")
                                                .price(21000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Pisto Manchego")
                                                .description("Guiso de tomate, calabacín, pimiento, cebolla y berenjena con aceite de oliva.")
                                                .price(22000.0)
                                                .category(categoryRepository.findByName("Entradas"))
                                                .imageUrl("https://images.unsplash.com/photo-1547592180-85f173990554?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Huevos Rotos con Jamón")
                                                .description("Huevos fritos sobre patatas caseras acompañados de láminas de jamón ibérico.")
                                                .price(29000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://images.unsplash.com/photo-1525351484163-7529414344d8?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(true).build());

                productRepository.save(
                                Product.builder().name("Bocadillo de Calamares")
                                                .description("Tradicional bocadillo madrileño de calamares fritos servido con alioli casero.")
                                                .price(23000.0)
                                                .category(categoryRepository.findByName("Mariscos"))
                                                .imageUrl("https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Cachopo Asturiano")
                                                .description("Filetes de ternera rellenos de jamón serrano y queso, empanados y fritos hasta quedar dorados.")
                                                .price(52000.0)
                                                .category(categoryRepository.findByName("Carnes"))
                                                .imageUrl("https://images.unsplash.com/photo-1544025162-d76694265947?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(true).build());

                productRepository.save(
                                Product.builder().name("Crema Catalana")
                                                .description("Postre tradicional catalán de crema suave con una fina capa de azúcar caramelizado.")
                                                .price(16000.0)
                                                .category(categoryRepository.findByName("Postres"))
                                                .imageUrl("https://images.unsplash.com/photo-1488477181946-6428a0291777?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Arroz con Leche")
                                                .description("Postre cremoso de arroz cocido lentamente con leche, canela y cáscara de limón.")
                                                .price(14000.0)
                                                .category(categoryRepository.findByName("Postres"))
                                                .imageUrl("https://images.unsplash.com/photo-1488477181946-6428a0291777?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Churros con Chocolate")
                                                .description("Churros españoles crujientes acompañados de una taza de chocolate caliente y espeso.")
                                                .price(18000.0)
                                                .category(categoryRepository.findByName("Postres"))
                                                .imageUrl("https://images.unsplash.com/photo-1624371414361-e670edf4898b?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(true).build());

                productRepository.save(
                                Product.builder().name("Torrijas")
                                                .description("Rebanadas de pan empapadas en leche aromatizada con canela y limón, doradas y espolvoreadas con azúcar.")
                                                .price(15000.0)
                                                .category(categoryRepository.findByName("Postres"))
                                                .imageUrl("https://images.unsplash.com/photo-1488477181946-6428a0291777?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Tarta de Queso Vasca")
                                                .description("Tarta de queso cremosa de interior suave y superficie caramelizada al estilo vasco.")
                                                .price(19000.0)
                                                .category(categoryRepository.findByName("Postres"))
                                                .imageUrl("https://images.unsplash.com/photo-1565958011703-44f9829ba187?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(true).build());

                productRepository.save(
                                Product.builder().name("Flan de Huevo")
                                                .description("Flan casero de huevo y leche servido con caramelo líquido y una textura delicadamente cremosa.")
                                                .price(14000.0)
                                                .category(categoryRepository.findByName("Postres"))
                                                .imageUrl("https://images.unsplash.com/photo-1488477181946-6428a0291777?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Sangría Española")
                                                .description("Bebida tradicional española preparada con vino tinto, frutas frescas y un toque cítrico.")
                                                .price(18000.0)
                                                .category(categoryRepository.findByName("Bebidas"))
                                                .imageUrl("https://images.unsplash.com/photo-1551024709-8f23befc6f87?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(true).build());

                productRepository.save(
                                Product.builder().name("Horchata de Chufa")
                                                .description("Bebida tradicional valenciana elaborada a base de chufa, fresca y ligeramente dulce.")
                                                .price(12000.0)
                                                .category(categoryRepository.findByName("Bebidas"))
                                                .imageUrl("https://images.unsplash.com/photo-1544145945-f90425340c7e?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Tinto de Verano")
                                                .description("Bebida refrescante preparada con vino tinto, gaseosa y un toque de limón.")
                                                .price(14000.0)
                                                .category(categoryRepository.findByName("Bebidas"))
                                                .imageUrl("https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(false).build());

                productRepository.save(
                                Product.builder().name("Café Cortado")
                                                .description("Café espresso servido con una pequeña cantidad de leche caliente.")
                                                .price(8000.0)
                                                .category(categoryRepository.findByName("Bebidas"))
                                                .imageUrl("https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=480&h=360&fit=crop&auto=format")
                                                .active(true).popular(true).build());
        }
}
