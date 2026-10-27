package com.example.demo.service;

import com.example.demo.dto.ProductOptionDTO;
import com.example.demo.errors.ApiException;
import com.example.demo.entities.OrderStatus;
import com.example.demo.entities.Product;
import com.example.demo.errors.ProductNotFoundException;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * Lógica de negocio de los productos: consultas para el cliente (solo activos),
 * CRUD del admin con sus validaciones, y reglas para eliminar (no se puede si
 * el producto está en pedidos).
 *
 * Todos los métodos son @Transactional, así que se ejecutan dentro de una
 * transacción y se pueden leer las relaciones sin problemas.
 */
@Service
public class ProductServiceImpl implements ProductService {

    // Acceso a la tabla de productos
    @Autowired
    private ProductRepository repository;

    // Se usa para saber si un producto aparece en pedidos antes de eliminarlo
    @Autowired
    private OrderRepository orderRepository;

    // Todos los productos (activos e inactivos), para el admin
    @Override
    @Transactional
    public Collection<Product> getAllProducts() {
        return repository.findAll();
    }

    // Solo los productos activos, para el cliente
    @Override
    @Transactional
    public Collection<Product> getAllProductsActive() {
        return repository.findByActiveTrue();
    }

    // Busca un producto por id (activo o no); si no existe lanza
    // ProductNotFoundException
    @Override
    @Transactional
    public Product getProductById(Integer id) {

        Product product = repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));

        return product;
    }

    // Igual que getProductById, pero además rechaza los productos inactivos.
    // Así el cliente no puede ver ni pedir un plato que el admin desactivó.
    @Override
    @Transactional
    public Product getActiveProductById(Integer id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        if (!product.getActive()) {
            throw new ProductNotFoundException(
                    "El producto " + product.getName() + " ya no está disponible.");
        }
        return product;
    }

    // Productos populares y activos, para "Favoritos de la casa" en el inicio
    @Override
    @Transactional
    public Collection<Product> getPopularProductsActive() {
        return repository.findByPopularTrueAndActiveTrue();
    }

    // Productos relacionados con uno dado, para "También te puede gustar"
    @Override
    @Transactional
    public Collection<Product> getRelatedProducts(Integer id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        // Un producto sin categoría no tiene con qué relacionarse
        if (product.getCategory() == null) {
            return List.of();
        }

        // Solo activos de la misma categoría, sin el propio producto; máximo 3
        return repository.findTop3ByCategoryIdAndActiveTrueAndIdNotOrderByIdAsc(
                product.getCategory().getId(), product.getId());
    }

    // Solo los ids de los productos activos (consulta liviana, sin traer los platos
    // completos)
    @Override
    @Transactional
    public List<Integer> getActiveProductIds() {
        return repository.findActiveIds();
    }

    // Solo id y nombre de todos los productos, para llenar desplegables de filtro
    @Override
    @Transactional
    public List<ProductOptionDTO> getProductOptions() {
        return repository.findOptions();
    }

    // Validaciones de negocio compartidas por addProduct() y updateProduct().
    private void validateProduct(Product product) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw ApiException.badRequest("El nombre del producto no puede estar vacío.");
        }
        if (product.getPrice() == null || product.getPrice() <= 0) {
            throw ApiException.badRequest("El precio del producto debe ser mayor que cero.");
        }
    }

    // Crea un producto nuevo. Si no se indica, nace activo y no popular.
    @Override
    @Transactional
    public Product addProduct(Product product) {

        validateProduct(product);

        if (product.getActive() == null) {
            product.setActive(true);
        }
        if (product.getPopular() == null) {
            product.setPopular(false);
        }
        return repository.save(product);
    }

    // Actualiza un producto existente con los datos del formulario del admin
    @Override
    @Transactional
    public Product updateProduct(Integer id, Product product) {

        // Se comprueba que exista antes de actualizar
        Product existingProduct = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        validateProduct(product);

        // Si el formulario no manda el estado, se conserva el actual
        if (product.getActive() == null) {
            product.setActive(existingProduct.getActive());
        }

        if (product.getPopular() == null) {
            product.setPopular(false);
        }
        // Se fuerza el id de la ruta para que se actualice este producto y no otro
        product.setId(id);

        return repository.save(product);
    }

    // Elimina un producto aplicando dos reglas, de la más urgente a la menos:
    // 1) Si está en un pedido que aún no se entregó, no se puede borrar.
    // 2) Si solo aparece en pedidos ya entregados (historial), tampoco se borra,
    // para no dejar el historial incompleto; se debe desactivar.
    @Override
    @Transactional
    public void deleteProduct(Integer id) {
        if (orderRepository.existsByDetailsProductIdAndStatusNot(id, OrderStatus.DELIVERED)) {
            throw ApiException.conflict(
                    "No se puede eliminar este producto porque está asociado a un pedido que aún no se ha entregado.");
        }
        if (orderRepository.existsByDetailsProductId(id)) {
            throw ApiException.conflict(
                    "No se puede eliminar este producto porque tiene pedidos asociados en el historial. Puedes desactivarlo.");
        }

        repository.deleteById(id);
    }

    // Cambia el producto de activo a inactivo, o al revés. Un producto inactivo
    // deja de verse en el menú pero se conserva en la base de datos.
    @Override
    @Transactional
    public Product toggleProductActive(Integer id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setActive(!product.getActive());

        return repository.save(product);
    }
}