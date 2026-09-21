package com.skilllink.repository;

import com.skilllink.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository
        extends JpaRepository<Service, Long> {

    List<Service> findByProviderEmail(String email);

    List<Service> findByCategory(String category);
}