package com.example.demo.service;

import java.util.Collection;

import com.example.demo.entities.UserRole;

public interface UserRoleService {

    Collection<UserRole> getAllUserRoles();

    UserRole getUserRoleById(Integer id);

    UserRole findByEmail(String email);

}