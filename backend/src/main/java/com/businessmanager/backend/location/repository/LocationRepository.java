package com.businessmanager.backend.location.repository;

import com.businessmanager.backend.location.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByCode(String code);
    Optional<Location> findByIsDefaultTrue(); // Custom lookup for default location
}
