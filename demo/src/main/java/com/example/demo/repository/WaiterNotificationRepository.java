package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entities.WaiterNotification;

@Repository
public interface WaiterNotificationRepository extends JpaRepository<WaiterNotification, Integer> {

    // Bandeja del mesero: los 50 avisos más recientes (usa el índice waiter + fecha)
    List<WaiterNotification> findTop50ByWaiterIdOrderByCreatedAtDescIdDesc(Integer waiterId);

    // Un aviso solo si es de ese mesero (nadie marca avisos de otro)
    Optional<WaiterNotification> findByIdAndWaiterId(Integer id, Integer waiterId);

    @Modifying
    @Query("update WaiterNotification n set n.read = true where n.waiter.id = :waiterId and n.read = false")
    int markAllRead(@Param("waiterId") Integer waiterId);
}
