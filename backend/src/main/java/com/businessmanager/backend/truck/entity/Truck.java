package com.businessmanager.backend.truck.entity;

import com.businessmanager.backend.common.entity.BaseEntity;
import com.businessmanager.backend.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "trucks")
@Getter
@Setter
@NoArgsConstructor
public class Truck extends BaseEntity {

    @Column(name = "registration_number", nullable = false, unique = true, length = 50)
    private String registrationNumber;

    @Column(name = "make", nullable = false, length = 100)
    private String make;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "capacity")
    private Double capacity;

    @Column(name = "fuel_type", length = 50)
    private String fuelType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_employee_id")
    private Employee driver;

    @Column(name = "last_service_date")
    private LocalDate lastServiceDate;
}
