package com.ipayz.ipayz_backend.repository;

import com.ipayz.ipayz_backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByAccountNumber(String accountNumber);
}
