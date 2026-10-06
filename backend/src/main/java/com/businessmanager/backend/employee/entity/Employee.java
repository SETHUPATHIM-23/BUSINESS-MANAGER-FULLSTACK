package com.businessmanager.backend.employee.entity;

import com.businessmanager.backend.common.entity.BaseEntity;
import com.businessmanager.backend.common.crypto.EncryptionConverter;
import com.businessmanager.backend.employee.enums.EmployeeStatus;
import com.businessmanager.backend.location.entity.Location;
import com.businessmanager.backend.accounting.entity.Account;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "employees")
@Getter
@Setter
public class Employee extends BaseEntity {

    @Column(name = "employee_code", nullable = false, unique = true, length = 50)
    private String employeeCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "contact_details", length = 255)
    private String contactDetails;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(name = "role_title", nullable = false, length = 100)
    private String roleTitle;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;
}
