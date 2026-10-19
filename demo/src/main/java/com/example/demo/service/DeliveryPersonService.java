package com.example.demo.service;

import com.example.demo.entities.DeliveryPerson;

import java.util.List;

public interface DeliveryPersonService {

    List<DeliveryPerson> getAvailableDeliveryPersons();
}