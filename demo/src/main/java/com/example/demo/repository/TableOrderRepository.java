package com.example.demo.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entities.TableOrder;
import com.example.demo.entities.TableOrderStatus;

import jakarta.persistence.LockModeType;

@Repository
public interface TableOrderRepository extends JpaRepository<TableOrder, Integer> {

    // Pedidos en esos estados con su mesa, mesero y platos en UNA consulta,
    // del más antiguo al más nuevo (usa el índice status + start_time).
    // La cocina la llama cada pocos segundos.
    @Query("""
            select distinct o from TableOrder o
            join fetch o.table
            join fetch o.waiter
            left join fetch o.items i
            left join fetch i.product
            where o.status in :statuses
            order by o.startTime asc
            """)
    List<TableOrder> findWithItemsByStatusIn(@Param("statuses") Collection<TableOrderStatus> statuses);

    // Pedidos de una mesa en esos estados, con sus platos (pantalla de la mesa del mesero)
    @Query("""
            select distinct o from TableOrder o
            join fetch o.waiter
            left join fetch o.items i
            left join fetch i.product
            where o.table.id = :tableId and o.status in :statuses
            order by o.startTime asc
            """)
    List<TableOrder> findWithItemsByTableAndStatusIn(@Param("tableId") Integer tableId,
            @Param("statuses") Collection<TableOrderStatus> statuses);

    // ¿La mesa tiene otro pedido en curso además de este?
    boolean existsByTableIdAndStatusInAndIdNot(Integer tableId, Collection<TableOrderStatus> statuses,
            Integer id);

    // Bloquea el pedido mientras se cambia su estado: evita que la cocina marque
    // un plato a la vez que el mesero lo cancela o lo entrega
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from TableOrder o where o.id = :id")
    Optional<TableOrder> lockById(@Param("id") Integer id);
}
