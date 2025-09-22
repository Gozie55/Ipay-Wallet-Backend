package com.ipayz.ipayz_backend.repository;

import com.ipayz.ipayz_backend.entity.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<WalletEntity, Long> {
    Optional<WalletEntity> findByUserId(Long userId);

    boolean existsByAccountNumber(String accountNumber);

    Optional<WalletEntity> findByAccountNumber(String accountNumber);
    Optional<WalletEntity> findFirstByUserId(Long userId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WalletEntity w where w.id = :id")
    Optional<WalletEntity> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WalletEntity w where w.accountNumber = :accountNumber")
    Optional<WalletEntity> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);
}
