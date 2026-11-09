package com.example.demo.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.AddressDTO;
import com.example.demo.entities.Address;
import com.example.demo.entities.Client;
import com.example.demo.errors.ApiException;
import com.example.demo.repository.AddressRepository;
import com.example.demo.repository.ClientRepository;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ClientRepository clientRepository;

    // Direcciones del cliente (la predeterminada primero); 404 si el cliente no
    // existe
    @Override
    @Transactional
    public List<AddressDTO> getAddressesByClientId(Integer clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw ApiException.notFound("No se encontró el cliente solicitado.");
        }
        return addressRepository.findByClientIdOrderByDefaultAddressDescIdAsc(clientId).stream()
                .map(this::toDTO)
                .toList();
    }

    // Agrega una dirección nueva al cliente
    @Override
    @Transactional
    public AddressDTO addAddress(Integer clientId, AddressDTO dto) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> ApiException.notFound("No se encontró el cliente solicitado."));

        String label = AddressRules.clean(dto.getLabel());
        checkDuplicateLabel(clientId, label, null);

        List<Address> existing = addressRepository.findByClientIdOrderByDefaultAddressDescIdAsc(clientId);

        // Sin nombre se asigna "Casa" (o "Casa 2"...); con nombre no puede repetirse
        String name = AddressRules.cleanName(dto.getName());
        if (name == null) {
            name = AddressRules.freeName(existing.stream().map(Address::getName).toList());
        } else {
            checkDuplicateName(clientId, name, null);
        }

        // La primera dirección del cliente es la predeterminada
        boolean makeDefault = existing.isEmpty() || Boolean.TRUE.equals(dto.getDefaultAddress());
        if (makeDefault) {
            existing.forEach(a -> a.setDefaultAddress(false));
        }

        Address address = Address.builder()
                .name(name)
                .label(label)
                .defaultAddress(makeDefault)
                .client(client)
                .build();
        return toDTO(addressRepository.save(address));
    }

    // Cambia el nombre, el texto o la marca de predeterminada; debe ser del cliente
    @Override
    @Transactional
    public AddressDTO updateAddress(Integer clientId, Integer addressId, AddressDTO dto) {
        Address address = findOwned(clientId, addressId);

        String label = AddressRules.clean(dto.getLabel());
        checkDuplicateLabel(clientId, label, addressId);

        String name = AddressRules.cleanName(dto.getName());
        if (name == null) {
            throw ApiException.badRequest("Escribe el nombre de la dirección.");
        }
        checkDuplicateName(clientId, name, addressId);

        // Las entidades están gestionadas: al terminar la transacción se guardan solas
        address.setLabel(label);
        address.setName(name);

        // Marcarla como predeterminada se la quita a las demás. Para cambiar la
        // predeterminada se marca otra: no se puede dejar al cliente sin ninguna.
        if (Boolean.TRUE.equals(dto.getDefaultAddress())
                && !Boolean.TRUE.equals(address.getDefaultAddress())) {
            addressRepository.findByClientIdOrderByDefaultAddressDescIdAsc(clientId)
                    .forEach(a -> a.setDefaultAddress(a.getId().equals(addressId)));
        }

        return toDTO(address);
    }

    // Elimina una dirección; debe ser del cliente
    @Override
    @Transactional
    public void deleteAddress(Integer clientId, Integer addressId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> ApiException.notFound("No se encontró el cliente solicitado."));

        Address removed = client.getAddresses().stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("La dirección no existe."));

        // Se quita de la lista del cliente y orphanRemoval la borra de la BD
        client.getAddresses().remove(removed);

        // Si era la predeterminada, la más antigua que quede toma su lugar
        if (Boolean.TRUE.equals(removed.getDefaultAddress())) {
            client.getAddresses().stream()
                    .min(Comparator.comparing(Address::getId))
                    .ifPresent(a -> a.setDefaultAddress(true));
        }
    }

    // Busca la dirección y comprueba que sea de ese cliente (si no, 404)
    private Address findOwned(Integer clientId, Integer addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> ApiException.notFound("La dirección no existe."));

        if (!address.getClient().getId().equals(clientId)) {
            throw ApiException.notFound("La dirección no existe.");
        }
        return address;
    }

    // ignoreId: al editar, la propia dirección no cuenta como duplicada
    private void checkDuplicateLabel(Integer clientId, String label, Integer ignoreId) {
        addressRepository.findByClientIdAndLabelIgnoreCase(clientId, label)
                .filter(a -> !a.getId().equals(ignoreId))
                .ifPresent(a -> {
                    throw ApiException.conflict("Ya tienes guardada esa dirección.");
                });
    }

    private void checkDuplicateName(Integer clientId, String name, Integer ignoreId) {
        addressRepository.findByClientIdAndNameIgnoreCase(clientId, name)
                .filter(a -> !a.getId().equals(ignoreId))
                .ifPresent(a -> {
                    throw ApiException.conflict("Ya tienes una dirección con ese nombre.");
                });
    }

    private AddressDTO toDTO(Address address) {
        return new AddressDTO(address.getId(), address.getName(), address.getLabel(),
                address.getDefaultAddress());
    }
}