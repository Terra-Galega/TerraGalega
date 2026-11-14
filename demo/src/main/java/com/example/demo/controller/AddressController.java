package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.AddressDTO;
import com.example.demo.security.AuthenticatedUser;
import com.example.demo.service.AddressService;

import io.swagger.v3.oas.annotations.Operation;

// Direcciones del cliente con sesión iniciada. El cliente se obtiene del token,
// así nadie puede ver ni modificar las direcciones de otra persona.
@RestController
@RequestMapping("/clients/me/addresses")
@CrossOrigin(origins = "http://localhost:4200")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @Autowired
    private AuthenticatedUser authenticatedUser;

    @Operation(summary = "Get the saved addresses of the logged-in client")
    @GetMapping
    public List<AddressDTO> getAddresses() {
        return addressService.getAddressesByClientId(authenticatedUser.client().getId());
    }

    @Operation(summary = "Add an address to the logged-in client")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressDTO addAddress(@RequestBody AddressDTO dto) {
        return addressService.addAddress(authenticatedUser.client().getId(), dto);
    }

    // El servicio comprueba que la dirección pertenezca al cliente
    @Operation(summary = "Update an address of the logged-in client")
    @PutMapping("/{addressId}")
    public AddressDTO updateAddress(@PathVariable Integer addressId, @RequestBody AddressDTO dto) {
        return addressService.updateAddress(authenticatedUser.client().getId(), addressId, dto);
    }

    @Operation(summary = "Delete an address of the logged-in client")
    @DeleteMapping("/{addressId}")
    public void deleteAddress(@PathVariable Integer addressId) {
        addressService.deleteAddress(authenticatedUser.client().getId(), addressId);
    }
}
