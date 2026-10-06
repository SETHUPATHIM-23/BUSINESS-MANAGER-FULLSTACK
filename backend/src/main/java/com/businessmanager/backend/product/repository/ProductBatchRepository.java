package com.businessmanager.backend.product.repository;

import com.businessmanager.backend.product.entity.ProductBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductBatchRepository extends JpaRepository<ProductBatch, Long> {
    List<ProductBatch> findByProductId(Long productId);
    List<ProductBatch> findByProductIdAndStockOnHandGreaterThan(Long productId, java.math.BigDecimal amount);
}
