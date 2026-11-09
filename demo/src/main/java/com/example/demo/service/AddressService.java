package com.example.demo.service;

import java.util.List;
import com.example.demo.dto.AddressDTO;

// Operaciones sobre las direcciones guardadas de un cliente.
public interface AddressService {

    List<AddressDTO> getAddressesByClientId(Integer clientId);

    AddressDTO addAddress(Integer clientId, AddressDTO dto);

    AddressDTO updateAddress(Integer clientId, Integer addressId, AddressDTO dto);

    void deleteAddress(Integer clientId, Integer addressId);
}