package com.businessmanager.backend.product.repository;

import com.businessmanager.backend.product.entity.Product;
import com.businessmanager.backend.product.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    /** Unassign a tax rate from all products that reference it (before deletion). */
    @Modifying
    @Query("UPDATE Product p SET p.taxRate = null WHERE p.taxRate.id = :taxRateId")
    void clearTaxRateById(@Param("taxRateId") Long taxRateId);

    @Query("SELECT p FROM Product p WHERE " +
           "(:search IS NULL OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:status IS NULL OR p.status = :status)")
    Page<Product> searchProducts(
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            @Param("status") ProductStatus status,
            Pageable pageable
    );

    long countByStatus(ProductStatus status);

    @Query("SELECT SUM(p.costPrice) FROM Product p WHERE p.includeInFinancialCalculations = true AND p.status = 'ACTIVE'")
    java.math.BigDecimal calculateActiveCatalogValuation();

    java.util.List<Product> findByIncludeInFinancialCalculationsTrue();

    @Query("SELECT p FROM Product p WHERE p.stockOnHand <= p.reorderLevel AND p.status = 'ACTIVE'")
    java.util.List<Product> findLowStockProducts();
}
