package com.example.demo.repository;

import com.example.demo.entities.AddOn;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface AddOnRepository extends JpaRepository<AddOn, Integer> {

}
