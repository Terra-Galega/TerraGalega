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
            left join fetch i.addOns
            where o.status in :statuses
            order by o.startTime asc
            """)
    List<TableOrder> findWithItemsByStatusIn(@Param("statuses") Collection<TableOrderStatus> statuses);

    // Pantalla de la mesa del mesero: pedidos en curso y los entregados que
    // faltan por cobrar, con sus platos
    @Query("""
            select distinct o from TableOrder o
            join fetch o.waiter
            left join fetch o.items i
            left join fetch i.product
            left join fetch i.addOns
            where o.table.id = :tableId
              and (o.status in :statuses
                   or (o.status = com.example.demo.entities.TableOrderStatus.DONE and o.paidAt is null))
            order by o.startTime asc
            """)
    List<TableOrder> findOpenWithItemsByTable(@Param("tableId") Integer tableId,
            @Param("statuses") Collection<TableOrderStatus> statuses);

    // Pedidos entregados y aún sin cobrar (portal del mesero: "por cobrar")
    @Query("""
            select o from TableOrder o join fetch o.table
            where o.status = com.example.demo.entities.TableOrderStatus.DONE and o.paidAt is null
            """)
    List<TableOrder> findAwaitingPayment();

    // "Cobrar mesa": los pedidos entregados por cobrar de una mesa, bloqueados
    // mientras se cobran (así no se cobran dos veces a la vez)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o from TableOrder o
            where o.table.id = :tableId
              and o.status = com.example.demo.entities.TableOrderStatus.DONE and o.paidAt is null
            order by o.startTime asc
            """)
    List<TableOrder> lockAwaitingPaymentByTable(@Param("tableId") Integer tableId);

    // Mesas ocupadas ahora en el salón y desde cuándo: [id de la mesa, start_time
    // de su pedido abierto más antiguo]. Abierto = en cocina o entregado sin cobrar.
    @Query("""
            select o.table.id, min(o.startTime) from TableOrder o
            where (o.status in :statuses
                   or (o.status = com.example.demo.entities.TableOrderStatus.DONE and o.paidAt is null))
            group by o.table.id
            """)
    List<Object[]> findOccupationStarts(@Param("statuses") Collection<TableOrderStatus> statuses);

    // ¿La mesa sigue ocupada? Tiene algo en cocina o algo entregado sin cobrar
    @Query("""
            select count(o) > 0 from TableOrder o
            where o.table.id = :tableId
              and (o.status in :statuses
                   or (o.status = com.example.demo.entities.TableOrderStatus.DONE and o.paidAt is null))
            """)
    boolean isTableOccupied(@Param("tableId") Integer tableId,
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
