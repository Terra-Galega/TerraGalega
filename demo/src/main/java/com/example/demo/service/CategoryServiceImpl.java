package com.example.demo.service;

import com.example.demo.entities.Category;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.entities.AddOn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository repository;

    @Override
    public Collection<Category> getAllCategorys() {
        return repository.findAll();
    }

    @Override
    public Category getCategoryById(Integer id) {
        return repository.findById(id);
    }

    @Override
    public Category getCategoryByName(String name) {
        return repository.findByName(name);
    }

    @Override
    public List<AddOn> getAddonsByCategory(String categoryName) {
        return repository.getAddonsByCategory(categoryName);
    }
}