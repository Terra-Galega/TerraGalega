package com.example.demo.repository;

import java.util.List;
import com.example.demo.dto.PointsDTOs.Movement;
import com.example.demo.entities.PointsMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointsMovementRepository extends JpaRepository<PointsMovement, Long> {
    @Query("select new com.example.demo.dto.PointsDTOs$Movement(m.order.id, m.kind, m.amount, m.createdAt) " +
           "from PointsMovement m where m.order.client.id = :clientId order by m.id desc")
    List<Movement> findHistory(@Param("clientId") Integer clientId);
}
