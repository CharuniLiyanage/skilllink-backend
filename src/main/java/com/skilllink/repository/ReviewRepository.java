package com.skilllink.repository;

import com.skilllink.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository
        extends JpaRepository<Review, Long> {

    Optional<Review> findByServiceRequestId(
            Long serviceRequestId
    );
}