package com.businessmanager.backend.customer.repository;

import com.businessmanager.backend.customer.entity.Customer;
import com.businessmanager.backend.customer.enums.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Look up customer by unique business key.
     */
    Optional<Customer> findByCustomerCode(String customerCode);

    /**
     * Filtered and paginated search for customer records.
     */
    @Query("SELECT c FROM Customer c WHERE " +
            "(:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:code IS NULL OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :code, '%'))) AND " +
            "(:phone IS NULL OR c.phone LIKE CONCAT('%', :phone, '%')) AND " +
            "(:status IS NULL OR c.status = :status)")
    Page<Customer> searchCustomers(
            @Param("name") String name,
            @Param("code") String code,
            @Param("phone") String phone,
            @Param("status") CustomerStatus status,
            Pageable pageable
    );

    /**
     * Retrieve the credit limit of a customer by ID.
     */
    @Query("SELECT c.creditLimit FROM Customer c WHERE c.id = :id")
    Optional<BigDecimal> findCreditLimitById(@Param("id") Long id);

    /**
     * Check if a credit hold is currently active on the customer.
     */
    @Query("SELECT c.creditHold FROM Customer c WHERE c.id = :id")
    Optional<Boolean> isCreditHoldActive(@Param("id") Long id);
}
