package com.example.demo.service;

import com.example.demo.entities.Admin;

import java.util.Collection;

public interface AdminService {

    Collection<Admin> getAllAdmins();

    Admin getAdminById(Integer id);

    Admin addAdmin(Admin admin);

    Admin findByEmail(String email);

}