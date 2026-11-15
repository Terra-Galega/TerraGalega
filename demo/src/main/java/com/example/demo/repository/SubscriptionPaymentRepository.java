package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entities.SubscriptionPayment;

@Repository
public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Integer> {

    List<SubscriptionPayment> findByClientIdAndPaymentState(Integer clientId, String paymentState);

    // El intento más reciente del cliente
    Optional<SubscriptionPayment> findFirstByClientIdOrderByIdDesc(Integer clientId);

    // Los usará el webhook y la tarea programada (paso 4)
    Optional<SubscriptionPayment> findByPaymentReference(String paymentReference);

    List<SubscriptionPayment> findByPaymentState(String paymentState);

    void deleteByClientId(Integer clientId);
}