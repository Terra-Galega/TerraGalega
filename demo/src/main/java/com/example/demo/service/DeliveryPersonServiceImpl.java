package com.example.demo.service;

import com.example.demo.entities.DeliveryPerson;
import com.example.demo.repository.DeliveryPersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DeliveryPersonServiceImpl implements DeliveryPersonService {

    @Autowired
    private DeliveryPersonRepository repository;

    // Solo domiciliarios activos y que no estén en un domicilio
    @Override
    @Transactional
    public List<DeliveryPerson> getAvailableDeliveryPersons() {
        return repository.findByAvailableTrueAndActiveTrue();
    }
}