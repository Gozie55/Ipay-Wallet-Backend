package com.ipayz.ipayz_backend.repository;

import com.ipayz.ipayz_backend.entity.OtpEntity;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpEntity, Long> {

    Optional<OtpEntity> findByEmailAndCode(String email, String code);

    Optional<OtpEntity> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, String purpose);

    void deleteAllByExpiryTimeBefore(Instant time);
}
