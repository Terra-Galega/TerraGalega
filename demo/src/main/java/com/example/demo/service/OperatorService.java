package com.example.demo.service;

import com.example.demo.entities.Operator;
import java.util.Collection;

public interface OperatorService {
    Collection<Operator> getAllOperators();

    Operator getOperatorById(Integer id);

    Operator addOperator(Operator operator);

    Operator updateOperator(Integer id, Operator operator);

    void deleteOperator(Integer id);

    // Devuelve null (no lanza excepción) si no existe; se usa, por ejemplo, para
    // validar el login.
    Operator findByEmail(String email);

    // Desactivar es la alternativa a eliminar cuando el operador tiene pedidos sin
    // completar.
    Operator toggleOperatorActive(Integer id);

    void changePassword(Integer id, String currentPassword, String newPassword);
}