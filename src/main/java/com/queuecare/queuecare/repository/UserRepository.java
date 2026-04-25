package com.queuecare.queuecare.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.queuecare.queuecare.model.User;

public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
