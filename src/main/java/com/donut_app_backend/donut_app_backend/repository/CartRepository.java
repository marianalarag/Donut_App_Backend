package com.donut_app_backend.donutapp.repository;

import com.donut_app_backend.donutapp.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    List<Cart> findByUserIdAndActiveTrue(Long userId);
    Optional<Cart> findByUserIdAndActiveTrueOrderByCreatedAtDesc(Long userId);
}