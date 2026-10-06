package com.businessmanager.backend.security.repository;

import com.businessmanager.backend.security.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    
    boolean existsByName(String name);

    @Query("SELECT r FROM Role r WHERE :search IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Role> searchRoles(@Param("search") String search, Pageable pageable);
}
