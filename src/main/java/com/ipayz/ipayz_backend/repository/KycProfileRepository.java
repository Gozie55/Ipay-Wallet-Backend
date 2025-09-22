package com.ipayz.ipayz_backend.repository;

import com.ipayz.ipayz_backend.entity.KycProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KycProfileRepository extends JpaRepository<KycProfileEntity, Long> {
    Optional<KycProfileEntity> findByUserId(Long userId);
}
