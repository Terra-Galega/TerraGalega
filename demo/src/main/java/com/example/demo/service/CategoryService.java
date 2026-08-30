package com.example.demo.service;

import com.example.demo.entities.Category;
import java.util.Collection;

public interface CategoryService {
    Collection<Category> getAllCategorys();

    Category getCategoryById(Integer id);

    Category getCategoryByName(String name);
}