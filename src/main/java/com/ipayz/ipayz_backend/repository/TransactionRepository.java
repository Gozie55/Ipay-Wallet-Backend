package com.ipayz.ipayz_backend.repository;

import com.ipayz.ipayz_backend.entity.TransactionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    Optional<TransactionEntity> findByReference(String reference);

    List<TransactionEntity> findByWallet_User_Id(Long userId);

    Optional<TransactionEntity> findByExternalReference(String externalReference);
    
    boolean existsByReference(String reference);

}
