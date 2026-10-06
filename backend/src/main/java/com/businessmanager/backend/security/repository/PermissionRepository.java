package com.businessmanager.backend.security.repository;

import com.businessmanager.backend.security.entity.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByCode(String code);
    
    boolean existsByCode(String code);

    @Query("SELECT p FROM Permission p WHERE " +
           "(:search IS NULL OR LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Permission> searchPermissions(@Param("search") String search, Pageable pageable);
}
