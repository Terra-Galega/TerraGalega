package com.example.demo.service;

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
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private OrderRepository orderRepository;

    @Override
    @Transactional
    public Collection<Product> getAllProducts() {
        return repository.findAll();
    }

    @Override
    @Transactional
    public Collection<Product> getAllProductsActive() {
        return repository.findByActiveTrue();
    }

    @Override
    @Transactional
    public Product getProductById(Integer id) {

        Product product = repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));

        return product;
    }

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

    @Override
    @Transactional
    public Collection<Product> getPopularProductsActive() {
        return repository.findByPopularTrueAndActiveTrue();
    }

    @Override
    @Transactional
    public Collection<Product> getRelatedProducts(Integer id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (product.getCategory() == null) {
            return List.of();
        }

        // Solo activos de la misma categoría, sin el propio producto; máximo 3
        return repository.findTop3ByCategoryIdAndActiveTrueAndIdNotOrderByIdAsc(
                product.getCategory().getId(), product.getId());
    }

    @Override
    @Transactional
    public List<Integer> getActiveProductIds() {
        return repository.findActiveIds();
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

    @Override
    @Transactional
    public Product updateProduct(Integer id, Product product) {

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
        product.setId(id);

        return repository.save(product);
    }

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

    @Override
    @Transactional
    public Product toggleProductActive(Integer id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setActive(!product.getActive());

        return repository.save(product);
    }
}