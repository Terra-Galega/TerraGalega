package com.example.demo.movil.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.movil.entity.CartLine;

@Repository
public interface CartLineRepository extends JpaRepository<CartLine, Long> {

    List<CartLine> findByClientIdOrderByIdAsc(Integer clientId);

    Optional<CartLine> findByClientIdAndLineId(Integer clientId, String lineId);
}