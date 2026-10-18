package com.example.demo.service;
import com.example.demo.entities.Operator;
import java.util.Collection;

public interface OperatorService {
    Collection<Operator> getAllOperators();

    Operator getOperatorById(Integer id);

    Operator addOperator(Operator operator);

    Operator updateOperator(Integer id, Operator operator);

    void deleteOperator(Integer id);

    Operator findByEmail(String email);
}
