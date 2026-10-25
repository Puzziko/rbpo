package com.example.rbpo.repository;

import com.example.rbpo.entity.SoftwareProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SoftwareProductRepository
        extends JpaRepository<SoftwareProduct, Long> {

    boolean existsByName(String name);
}