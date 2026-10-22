package com.example.demo.repository;

import com.example.demo.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.entities.OrderStatus;
import java.util.Collection;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findByClientIdOrderByCreatedAtDesc(Integer clientId);

    boolean existsByDetailsOrderDetailAddOnsAddOnId(Integer addOnId);

    boolean existsByDetailsProductIdAndStatusNot(Integer productId, OrderStatus status);

    boolean existsByDetailsProductId(Integer productId);

    boolean existsByOperatorId(Integer operatorId);

    List<Order> findByStatusNotInOrderByCreatedAtAsc(Collection<OrderStatus> statuses);

    List<Order> findAllByOrderByCreatedAtDesc();

    boolean existsByDeliveryPersonId(Integer deliveryPersonId);

    boolean existsByOperatorIdAndStatusNotIn(Integer operatorId, Collection<OrderStatus> statuses);

    List<Order> findByOperatorId(Integer operatorId);

    boolean existsByDeliveryPersonIdAndStatusNotIn(Integer deliveryPersonId, Collection<OrderStatus> statuses);

    List<Order> findByDeliveryPersonId(Integer deliveryPersonId);
}