package com.businessmanager.backend.product.service;

import com.businessmanager.backend.product.entity.Product;
import com.businessmanager.backend.product.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    Product createProduct(Product product, Long categoryId, Long taxRateId);

    Product getProductById(Long id);

    Product getProductBySku(String sku);

    Page<Product> searchProducts(String search, Long categoryId, ProductStatus status, Pageable pageable);

    Product updateProduct(Long id, Product product, Long categoryId, Long taxRateId);

    void deactivateProduct(Long id);

    void deleteProduct(Long id);

    java.math.BigDecimal resolvePrice(Long productId, Long customerId);

    java.math.BigDecimal getValuationOfFinancialCalculations();

    com.businessmanager.backend.product.dto.ProductImportResultDto importProducts(
            java.util.List<com.businessmanager.backend.product.dto.ProductCreateDto> dtos
    );

    String exportProductsToCsv();
}
