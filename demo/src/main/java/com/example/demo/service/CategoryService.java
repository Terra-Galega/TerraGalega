package com.example.demo.service;

import com.example.demo.entities.Category;
import com.example.demo.entities.AddOn;
import java.util.Collection;
import java.util.List;

public interface CategoryService {
    Collection<Category> getAllCategorys();

    Category getCategoryById(Integer id);

    Category getCategoryByName(String name);

    List<AddOn> getAddonsByCategory(String categoryName);
}