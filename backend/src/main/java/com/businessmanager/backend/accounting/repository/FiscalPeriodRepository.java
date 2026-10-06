package com.businessmanager.backend.accounting.repository;

import com.businessmanager.backend.accounting.entity.FiscalPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface FiscalPeriodRepository extends JpaRepository<FiscalPeriod, Long> {

    Optional<FiscalPeriod> findByName(String name);

    @Query("SELECT fp FROM FiscalPeriod fp WHERE :date BETWEEN fp.startDate AND fp.endDate")
    Optional<FiscalPeriod> findActivePeriodForDate(@Param("date") LocalDate date);
}
