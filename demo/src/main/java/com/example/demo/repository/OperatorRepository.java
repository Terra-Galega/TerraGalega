package com.example.demo.repository;

import com.example.demo.entities.Operator;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface OperatorRepository extends JpaRepository<Operator, Integer> {

    Operator findByEmail(String email);

}