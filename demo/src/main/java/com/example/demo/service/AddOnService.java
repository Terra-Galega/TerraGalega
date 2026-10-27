package com.example.demo.service;

import com.example.demo.entities.AddOn;
import java.util.Collection;

/**
 * Contrato de la lógica de negocio de los adicionales (addons).
 * La implementación está en AddOnServiceImpl.
 */
public interface AddOnService {

    // Todos los adicionales, activos e inactivos (vista del admin)
    Collection<AddOn> getAllAddOns();

    // Solo los adicionales activos
    Collection<AddOn> getAllAddOnsActive();

    // Un adicional por id; falla si no existe
    AddOn getAddOnById(Integer id);

    // Adicionales activos que pertenecen a una categoría (para el detalle de un
    // plato)
    Collection<AddOn> getActiveAddOnsByCategory(Integer categoryId);

    // Crea un adicional nuevo, validando sus datos
    AddOn addAddOn(AddOn addOn);

    // Actualiza un adicional y las categorías en las que aparece
    AddOn updateAddOn(Integer id, AddOn addOn);

    // Elimina un adicional (no se permite si ya está en pedidos)
    void deleteAddOn(Integer id);

    // Alterna el adicional entre activo e inactivo
    AddOn toggleAddOnActive(Integer id);
}