package com.example.demo.controller;

import com.example.demo.entities.AddOn;
import com.example.demo.service.AddOnService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/addons")
@CrossOrigin(origins = "http://localhost:4200")
public class AddOnController {

    @Autowired
    private AddOnService addOnService;

    @Operation(summary = "Get all addons (only for admin)")
    @GetMapping
    public Collection<AddOn> getAllAddOns() {
        return addOnService.getAllAddOns();
    }

    @Operation(summary = "Get all active addons")
    @GetMapping("/active")
    public Collection<AddOn> getActiveAddOns() {
        return addOnService.getAllAddOnsActive();
    }

    @Operation(summary = "Get addon by ID")
    @GetMapping("/{id}")
    public AddOn getAddOnById(@PathVariable Integer id) {
        return addOnService.getAddOnById(id);
    }

    @Operation(summary = "Add a new addon (only for admin)")
    @PostMapping
    public AddOn addAddOn(@RequestBody AddOn addOn) {
        return addOnService.addAddOn(addOn);
    }

    @Operation(summary = "Update an addon (only for admin)")
    @PutMapping("/{id}")
    public AddOn updateAddOn(@PathVariable Integer id, @RequestBody AddOn addOn) {
        return addOnService.updateAddOn(id, addOn);
    }

    @Operation(summary = "Delete an addon (only for admin)")
    @DeleteMapping("/{id}")
    public void deleteAddOn(@PathVariable Integer id) {
        addOnService.deleteAddOn(id);
    }

    @Operation(summary = "Toggle addon active status (only for admin)")
    @PutMapping("/{id}/toggle")
    public AddOn toggleAddOn(@PathVariable Integer id) {
        return addOnService.toggleAddOnActive(id);
    }
}