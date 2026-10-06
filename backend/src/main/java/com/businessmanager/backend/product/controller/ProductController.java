package com.businessmanager.backend.product.controller;

import com.businessmanager.backend.product.dto.*;
import com.businessmanager.backend.product.entity.Product;
import com.businessmanager.backend.product.enums.ProductStatus;
import com.businessmanager.backend.product.mapper.ProductMapper;
import com.businessmanager.backend.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;

    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    public ResponseEntity<ProductDetailDto> createProduct(@Valid @RequestBody ProductCreateDto dto) {
        Product product = productMapper.toEntity(dto);
        Product saved = productService.createProduct(product, dto.getCategoryId(), dto.getTaxRateId());
        return ResponseEntity.status(HttpStatus.CREATED).body(productMapper.toDetailDto(saved));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    public ResponseEntity<ProductDetailDto> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(productMapper.toDetailDto(product));
    }

    @GetMapping("/sku/{sku}")
    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    public ResponseEntity<ProductDetailDto> getProductBySku(@PathVariable String sku) {
        Product product = productService.getProductBySku(sku);
        return ResponseEntity.ok(productMapper.toDetailDto(product));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    public ResponseEntity<Page<ProductSummaryDto>> searchProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "sku") Pageable pageable
    ) {
        ProductStatus productStatus = null;
        if (status != null && !status.isEmpty()) {
            productStatus = ProductStatus.valueOf(status.toUpperCase());
        }
        Page<Product> productsPage = productService.searchProducts(search, categoryId, productStatus, pageable);
        return ResponseEntity.ok(productsPage.map(productMapper::toSummaryDto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    public ResponseEntity<ProductDetailDto> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateDto dto
    ) {
        Product product = productMapper.toEntity(dto);
        Product updated = productService.updateProduct(id, product, dto.getCategoryId(), dto.getTaxRateId());
        return ResponseEntity.ok(productMapper.toDetailDto(updated));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    public ResponseEntity<Void> deactivateProduct(@PathVariable Long id) {
        productService.deactivateProduct(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/resolve-price")
    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    public ResponseEntity<BigDecimal> resolvePrice(
            @PathVariable Long id,
            @RequestParam(required = false) Long customerId
    ) {
        BigDecimal price = productService.resolvePrice(id, customerId);
        return ResponseEntity.ok(price);
    }

    @GetMapping("/valuation")
    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    public ResponseEntity<BigDecimal> getValuationOfFinancialCalculations() {
        BigDecimal valuation = productService.getValuationOfFinancialCalculations();
        return ResponseEntity.ok(valuation);
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    public ResponseEntity<com.businessmanager.backend.product.dto.ProductImportResultDto> importProducts(
            @RequestBody java.util.List<ProductCreateDto> dtos
    ) {
        com.businessmanager.backend.product.dto.ProductImportResultDto report = productService.importProducts(dtos);
        return ResponseEntity.ok(report);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    public ResponseEntity<String> exportCsv() {
        String csv = productService.exportProductsToCsv();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"products.csv\"")
                .body(csv);
    }
}
