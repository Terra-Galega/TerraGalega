package com.example.demo.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.example.demo.dto.AddressDTO;
import com.example.demo.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/clients/{clientId}/addresses")
@CrossOrigin(origins = "http://localhost:4200")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @Operation(summary = "Get the saved addresses of a client")
    @GetMapping
    public List<AddressDTO> getAddresses(@PathVariable Integer clientId) {
        return addressService.getAddressesByClientId(clientId);
    }

    @Operation(summary = "Add an address to a client")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressDTO addAddress(@PathVariable Integer clientId,
            @RequestBody AddressDTO dto) {
        return addressService.addAddress(clientId, dto);
    }

    @Operation(summary = "Update an address of a client")
    @PutMapping("/{addressId}")
    public AddressDTO updateAddress(@PathVariable Integer clientId,
            @PathVariable Integer addressId,
            @RequestBody AddressDTO dto) {
        return addressService.updateAddress(clientId, addressId, dto);
    }

    @Operation(summary = "Delete an address of a client")
    @DeleteMapping("/{addressId}")
    public void deleteAddress(@PathVariable Integer clientId,
            @PathVariable Integer addressId) {
        addressService.deleteAddress(clientId, addressId);
    }
}