package com.example.demo.repository;

import com.example.demo.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.entities.OrderStatus;
import java.util.Collection;
import java.util.List;

/**
 * Repositorio de pedidos. Extiende JpaRepository, así que ya trae save,
 * findById, findAll, deleteById, etc. Los métodos
 * de abajo son consultas derivadas:Spring genera la consulta SQL a partir del nombre del método.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    // Historial de un cliente, del pedido más reciente al más antiguo
    List<Order> findByClientIdOrderByCreatedAtDesc(Integer clientId);

    // ¿Algún pedido usó este adicional? (para impedir borrar un adicional en uso)
    boolean existsByDetailsOrderDetailAddOnsAddOnId(Integer addOnId);

    // ¿Hay pedidos con este producto que no estén en el estado indicado?
    boolean existsByDetailsProductIdAndStatusNot(Integer productId, OrderStatus status);

    // ¿Algún pedido contiene este producto? (para impedir borrar un producto en uso)
    boolean existsByDetailsProductId(Integer productId);

    // ¿Este operador ha cocinado algún pedido alguna vez?
    boolean existsByOperatorId(Integer operatorId);

    // Pedidos que NO están en los estados dados, del más antiguo al más reciente.
    // Se usa para la lista de pedidos activos del operador
    List<Order> findByStatusNotInOrderByCreatedAtAsc(Collection<OrderStatus> statuses);

    // Todos los pedidos, del más reciente al más antiguo (vista del admin)
    List<Order> findAllByOrderByCreatedAtDesc();

    // ¿Este domiciliario ha tenido algún pedido asignado alguna vez?
    boolean existsByDeliveryPersonId(Integer deliveryPersonId);

    // ¿Este operador tiene pedidos sin terminar? (bloquea su eliminación)
    boolean existsByOperatorIdAndStatusNotIn(Integer operatorId, Collection<OrderStatus> statuses);

    // Todos los pedidos de un operador (para soltar la relación al eliminarlo)
    List<Order> findByOperatorId(Integer operatorId);

    // ¿Este domiciliario tiene pedidos sin terminar? (bloquea su eliminación)
    boolean existsByDeliveryPersonIdAndStatusNotIn(Integer deliveryPersonId, Collection<OrderStatus> statuses);

    // Todos los pedidos de un domiciliario (para soltar la relación al eliminarlo)
    List<Order> findByDeliveryPersonId(Integer deliveryPersonId);
}