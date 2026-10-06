package com.businessmanager.backend.accounting.repository;

import com.businessmanager.backend.accounting.entity.AccountMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountMappingRepository extends JpaRepository<AccountMapping, Long> {
    Optional<AccountMapping> findByMappingKey(String mappingKey);
}
