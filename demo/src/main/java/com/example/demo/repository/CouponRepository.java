package com.example.demo.repository;

import com.example.demo.entities.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Integer> {

    // Búsqueda por código (ya en mayúsculas). Devuelve null si no existe.
    Coupon findByCode(String code);

    // Cupones personales de un cliente
    List<Coupon> findByClientId(Integer clientId);

    // Cupones personales del cliente + cupones generales (sin dueño)
    @Query("select c from Coupon c left join c.client cl where cl is null or cl.id = :clientId")
    List<Coupon> findForClient(@Param("clientId") Integer clientId);

    // Se usa al eliminar la cuenta de un cliente
    void deleteByClientId(Integer clientId);
}