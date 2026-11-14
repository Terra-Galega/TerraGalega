package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.MapAddressDTO;
import com.example.demo.service.MapAddressService;

@RestController
@RequestMapping("/maps")
public class MapAddressController {

    @Autowired
    private MapAddressService service;

    @GetMapping("/search")
    public MapAddressDTO search(@RequestParam String q) {
        return service.search(q);
    }

    @GetMapping("/reverse")
    public MapAddressDTO reverse(
            @RequestParam double lat,
            @RequestParam double lon) {

        return service.reverse(lat, lon);
    }
}