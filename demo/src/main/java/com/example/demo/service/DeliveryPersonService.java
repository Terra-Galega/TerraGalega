package com.example.demo.service;

import com.example.demo.entities.DeliveryPerson;

import java.util.List;

public interface DeliveryPersonService {

    List<DeliveryPerson> getAllDeliveryPersons();

    List<DeliveryPerson> getAvailableDeliveryPersons();

    DeliveryPerson getDeliveryPersonById(Integer id);

    DeliveryPerson addDeliveryPerson(DeliveryPerson deliveryPerson);

    DeliveryPerson updateDeliveryPerson(Integer id, DeliveryPerson deliveryPerson);

    DeliveryPerson toggleDeliveryPersonActive(Integer id);

    void deleteDeliveryPerson(Integer id);

    DeliveryPerson findByEmail(String email);
}