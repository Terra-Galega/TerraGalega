package com.example.demo.service;

import com.example.demo.entities.AddOn;
import java.util.Collection;

public interface AddOnService {

    Collection<AddOn> getAllAddOns();

    Collection<AddOn> getAllAddOnsActive();

    AddOn getAddOnById(Integer id);

    AddOn addAddOn(AddOn addOn);

    AddOn updateAddOn(Integer id, AddOn addOn);

    void deleteAddOn(Integer id);

    AddOn toggleAddOnActive(Integer id);
}