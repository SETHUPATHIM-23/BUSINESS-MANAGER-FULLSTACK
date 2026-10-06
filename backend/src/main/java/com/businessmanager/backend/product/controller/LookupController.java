package com.businessmanager.backend.product.controller;

import com.businessmanager.backend.product.entity.ProductCategory;
import com.businessmanager.backend.product.entity.TaxRate;
import com.businessmanager.backend.product.repository.ProductCategoryRepository;
import com.businessmanager.backend.product.repository.ProductRepository;
import com.businessmanager.backend.product.repository.TaxRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lookups")
@RequiredArgsConstructor
public class LookupController {

    private final ProductCategoryRepository categoryRepository;
    private final TaxRateRepository taxRateRepository;
    private final ProductRepository productRepository;

    @GetMapping("/categories")
    @PreAuthorize("hasAnyAuthority('PRODUCT_READ', 'SYSTEM_READ', 'ADMIN')")
    public ResponseEntity<List<ProductCategory>> getCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @GetMapping("/tax-rates")
    @PreAuthorize("hasAnyAuthority('PRODUCT_READ', 'SYSTEM_READ', 'ADMIN')")
    public ResponseEntity<List<TaxRate>> getTaxRates() {
        return ResponseEntity.ok(taxRateRepository.findAll());
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAnyAuthority('PRODUCT_WRITE', 'SYSTEM_WRITE', 'ADMIN')")
    public ResponseEntity<ProductCategory> createCategory(@org.springframework.web.bind.annotation.RequestBody ProductCategory category) {
        if (categoryRepository.findByName(category.getName()).isPresent()) {
            throw new com.businessmanager.backend.common.exception.BusinessRuleException("Category name already exists: " + category.getName());
        }
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(categoryRepository.save(category));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAnyAuthority('PRODUCT_WRITE', 'SYSTEM_WRITE', 'ADMIN')")
    public ResponseEntity<Void> deleteCategory(@org.springframework.web.bind.annotation.PathVariable Long id) {
        ProductCategory cat = categoryRepository.findById(id)
                .orElseThrow(() -> new com.businessmanager.backend.common.exception.ResourceNotFoundException("Category not found with ID: " + id));
        categoryRepository.delete(cat);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tax-rates")
    @PreAuthorize("hasAnyAuthority('PRODUCT_WRITE', 'SYSTEM_WRITE', 'ADMIN')")
    public ResponseEntity<TaxRate> createTaxRate(@org.springframework.web.bind.annotation.RequestBody TaxRate taxRate) {
        if (taxRateRepository.findByName(taxRate.getName()).isPresent()) {
            throw new com.businessmanager.backend.common.exception.BusinessRuleException("Tax rate name already exists: " + taxRate.getName());
        }
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(taxRateRepository.save(taxRate));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/tax-rates/{id}")
    @PreAuthorize("hasAnyAuthority('PRODUCT_WRITE', 'SYSTEM_WRITE', 'ADMIN')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> deleteTaxRate(@org.springframework.web.bind.annotation.PathVariable Long id) {
        TaxRate taxRate = taxRateRepository.findById(id)
                .orElseThrow(() -> new com.businessmanager.backend.common.exception.ResourceNotFoundException("Tax rate not found with ID: " + id));

        // Unassign this tax rate from any products that reference it, then delete
        productRepository.clearTaxRateById(id);

        taxRateRepository.delete(taxRate);
        return ResponseEntity.noContent().build();
    }
}
