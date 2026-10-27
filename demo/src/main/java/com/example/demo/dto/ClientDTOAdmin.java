package com.example.demo.dto;

// DTO que contiene los datos del cliente que se muestran en el panel del admin.
public class ClientDTOAdmin {

    // Identificador del cliente.
    private Integer id;

    // Nombre del cliente.
    private String name;

    // Correo electrónico del cliente.
    private String email;

    // Número de teléfono del cliente.
    private String phone;

    // Devuelve el identificador del cliente.
    public Integer getId() {
        return id;
    }

    // Asigna el identificador del cliente.
    public void setId(Integer id) {
        this.id = id;
    }

    // Devuelve el nombre del cliente.
    public String getName() {
        return name;
    }

    // Asigna el nombre del cliente.
    public void setName(String name) {
        this.name = name;
    }

    // Devuelve el correo electrónico del cliente.
    public String getEmail() {
        return email;
    }

    // Asigna el correo electrónico del cliente.
    public void setEmail(String email) {
        this.email = email;
    }

    // Devuelve el número de teléfono del cliente.
    public String getPhone() {
        return phone;
    }

    // Asigna el número de teléfono del cliente.
    public void setPhone(String phone) {
        this.phone = phone;
    }
}