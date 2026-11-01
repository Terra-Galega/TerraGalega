package com.example.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.example.demo.dto.ProductOptionDTO;
import com.example.demo.entities.Category;
import com.example.demo.entities.Product;

/*
Pruebas de las consultas con @Query de ProductRepository
(findActiveIds, findOptions y findTop3...).
 */
@DataJpaTest
class ProductRepositoryQueriesTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;
    //variables globales
    private Category tortillas;
    private Category bebidas;
    private Product tortilla1;
    private Product tortilla2;
    private Product tortilla3;
    private Product tortillaInactiva;
    private Product tortilla4;
    private Product tortilla5;
    private Product limonada;

    // Constructor de un producto válido de una categoría con el estado dado
    private Product saveProduct(String name, Category category, boolean active) {
        return productRepository.save(Product.builder()
                .name(name)
                .price(20000.0)
                .category(category)
                .active(active)
                .popular(false)
                .build());
    }

    // Antes de cada prueba: 2 categorías y 7 productos (6 activos y 1 inactivo).
    // El orden de creación importa porque los ids crecen en ese orden.
    @BeforeEach
    void setUp() {
        tortillas = categoryRepository.save(Category.builder().name("Tortillas").build());
        bebidas = categoryRepository.save(Category.builder().name("Bebidas").build());

        tortilla1 = saveProduct("Tortilla 1", tortillas, true);
        tortilla2 = saveProduct("Tortilla 2", tortillas, true);
        tortillaInactiva = saveProduct("Tortilla Inactiva", tortillas, false);
        tortilla3 = saveProduct("Tortilla 3", tortillas, true);
        tortilla4 = saveProduct("Tortilla 4", tortillas, true);
        tortilla5 = saveProduct("Tortilla 5", tortillas, true);
        limonada = saveProduct("Limonada", bebidas, true);
    }

    @Test
    void ProductRepository_FindActiveIds_ReturnsOnlyActiveProductIds() {
        // Arrange: setUp() dejó 6 productos activos y 1 inactivo

        // Act
        List<Integer> ids = productRepository.findActiveIds();

        // Assert: están los activos y no aparece el inactivo
        assertThat(ids).hasSize(6);
        //revisar que tenga todos los ids correspondientes a los productos activos y que no tenga el id del producto inactivo
        assertThat(ids).contains(tortilla1.getId(), tortilla2.getId(), tortilla3.getId(),
                tortilla4.getId(), tortilla5.getId(), limonada.getId());
        assertThat(ids).doesNotContain(tortillaInactiva.getId());
    }

    @Test
    void ProductRepository_FindOptions_ReturnsIdAndNameOfAllProducts() {
        // Arrange: setUp() dejó 7 productos en total (incluido el inactivo)

        // Act
        List<ProductOptionDTO> options = productRepository.findOptions();

        // Assert: devuelve todos, con su id y nombre
        assertThat(options).hasSize(7);
        assertThat(options).extracting(ProductOptionDTO::name)
                .contains("Tortilla 1", "Tortilla Inactiva", "Limonada");
        assertThat(options).extracting(ProductOptionDTO::id)
                .contains(tortilla1.getId(), tortillaInactiva.getId(), limonada.getId());
    }

    @Test
    void ProductRepository_FindTop3ByCategory_ReturnsThreeActiveProductsExcludingGivenOne() {
        // Arrange: se piden relacionados de Tortilla 1 dentro de la categoría Tortillas
        Integer categoryId = tortillas.getId();
        Integer excludedId = tortilla1.getId();

        // Act
        List<Product> related = productRepository
                .findTop3ByCategoryIdAndActiveTrueAndIdNotOrderByIdAsc(categoryId, excludedId);

        // Assert: salta Tortilla 1 (excluida) y Tortilla Inactiva, respeta el límite de 3
        // (Tortilla 5 queda fuera) y los devuelve en orden de id
        assertThat(related).hasSize(3);
        assertThat(related).extracting(Product::getName)
                .containsExactly("Tortilla 2", "Tortilla 3", "Tortilla 4");
    }
}