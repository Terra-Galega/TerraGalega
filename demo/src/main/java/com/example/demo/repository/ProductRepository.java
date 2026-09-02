package com.example.demo.repository;

import com.example.demo.entities.Product;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Repository
public class ProductRepository {
        private final Map<Integer, Product> productMap = new HashMap<>();

        // DB quemada
        public ProductRepository() {

                productMap.put(1, new Product(
                                1,
                                "Pulpo a la Gallega",
                                "Tierno pulpo cocido sobre cama de patatas con pimentón de la Vera y aceite de oliva virgen extra.",
                                38900.0,
                                "Entradas",
                                "https://images.unsplash.com/photo-1534080564583-6be75777b70a?w=480&h=360&fit=crop&auto=format",

                                true,
                                true));

                // 2. Croquetas de Jamón Ibérico
                productMap.put(2, new Product(
                                2,
                                "Croquetas de Jamón Ibérico",
                                "Cremosas croquetas de jamón ibérico con bechamel artesanal, doradas en aceite de oliva de arbequina.",
                                24500.0,
                                "Entradas",
                                "https://images.unsplash.com/photo-1588276552401-30058a0fe57b?w=480&h=360&fit=crop&auto=format",
                                true,
                                false));

                // 3. Empanada Gallega
                productMap.put(3, new Product(
                                3,
                                "Empanada Gallega",
                                "Empanada tradicional con atún del norte, pimientos asados y cebolla caramelizada al estilo gallego.",
                                22000.0,
                                "Entradas",
                                "https://images.unsplash.com/photo-1650964807311-970cb88d347c?w=480&h=360&fit=crop&auto=format",
                                true,
                                false));

                // 4. Gambas a la Plancha
                productMap.put(4, new Product(
                                4,
                                "Gambas a la Plancha",
                                "Langostinos frescos a la plancha con mantequilla de ajo, perejil fresco y limón de Murcia.",
                                48500.0,
                                "Mariscos",
                                "https://images.unsplash.com/photo-1515443961218-a51367888e4b?w=480&h=360&fit=crop&auto=format",
                                true,
                                true));

                // 5. Mariscos al Ajillo
                productMap.put(5, new Product(
                                5,
                                "Mariscos al Ajillo",
                                "Selección de mariscos frescos salteados en aceite de ajo con vino Albariño y guindilla roja.",
                                62000.0,
                                "Mariscos",
                                "https://images.unsplash.com/photo-1621841957884-1210fe19d66d?w=480&h=360&fit=crop&auto=format",
                                true,
                                false));

                // 6. Paella de Mariscos
                productMap.put(6, new Product(
                                6,
                                "Paella de Mariscos",
                                "Arroz bomba al azafrán con gambas reales, mejillones, almejas y calamar fresco de la costa gallega.",
                                75000.0,
                                "Mariscos",
                                "https://images.unsplash.com/photo-1783685739826-335e8133a197?w=480&h=360&fit=crop&auto=format",
                                true,
                                true));

                // 7. Lacón con Grelos
                productMap.put(7, new Product(
                                7,
                                "Lacón con Grelos",
                                "Codillo gallego curado, chorizo ahumado y grelos tiernos cocidos lentamente en caldo de verduras.",
                                55000.0,
                                "Carnes",
                                "https://images.unsplash.com/photo-1623961990059-28356e226a77?w=480&h=360&fit=crop&auto=format",
                                true,
                                false));

                // 8. Solomillo a la Brasa
                productMap.put(8, new Product(
                                8,
                                "Solomillo a la Brasa",
                                "Solomillo Angus a la brasa con chimichurri gallego y reducción de vino Ribeiro sobre pizarra.",
                                82000.0,
                                "Carnes",
                                "https://images.unsplash.com/photo-1554371650-4484f3a102f2?w=480&h=360&fit=crop&auto=format",
                                true,
                                false));

                // 9. Tarta de Santiago
                productMap.put(9, new Product(
                                9,
                                "Tarta de Santiago",
                                "Clásica tarta de almendra gallega, aromatizada con limón y canela, decorada con la Cruz de Santiago.",
                                18000.0,
                                "Postres",
                                "https://images.unsplash.com/photo-1534080564583-6be75777b70a?w=480&h=360&fit=crop&auto=format",
                                true,
                                false));

                // 10. Filloas con Crema
                productMap.put(10, new Product(
                                10,
                                "Filloas con Crema",
                                "Filloas gallegas delicadas rellenas de crema de vainilla con coulis de frutos rojos del bosque.",
                                16500.0,
                                "Postres",
                                "https://images.unsplash.com/photo-1588276552401-30058a0fe57b?w=480&h=360&fit=crop&auto=format",
                                true,
                                false));
        }

        // Métodos para acceder a los productos
        // Devuelve todos los productos
        public Collection<Product> findAll() {
                return productMap.values();
        }

        // Devuelve un producto(comida) por su id
        public Product findById(Integer id) {
                return productMap.get(id);
        }

        // Calcula el siguiente id disponible (el mayor id actual + 1)
        private Integer nextId() {
                return productMap.keySet().stream().max(Integer::compareTo).orElse(0) + 1;
        }

        // Guarda un producto nuevo (le asigna id, lo marca activo por defecto) y lo
        // devuelve
        public Product save(Product product) {
                product.setId(nextId());
                if (product.getActive() == null) {
                        product.setActive(true);
                }
                productMap.put(product.getId(), product);
                return product;
        }

        // Actualiza los datos editables de un producto existente sin perder su id ni su
        // estado activo/inactivo
        public Product update(Integer id, Product product) {
                Product existing = productMap.get(id);
                if (existing == null) {
                        return null;
                }
                product.setId(id);
                product.setActive(existing.getActive());
                productMap.put(id, product);
                return product;
        }

        // Elimina un producto por su id
        public void deleteById(Integer id) {
                productMap.remove(id);
        }

        public Product toggleActive(Integer id) {
                Product product = productMap.get(id);
                if (product == null) {
                        return null;
                }
                boolean current = Boolean.TRUE.equals(product.getActive());
                product.setActive(!current);
                return product;
        }
}