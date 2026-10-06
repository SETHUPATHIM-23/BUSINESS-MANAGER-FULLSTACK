package com.businessmanager.backend.product.service;

import com.businessmanager.backend.common.audit.annotation.AuditAction;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.product.entity.Product;
import com.businessmanager.backend.product.entity.ProductCategory;
import com.businessmanager.backend.product.entity.TaxRate;
import com.businessmanager.backend.product.enums.ProductStatus;
import com.businessmanager.backend.product.repository.ProductCategoryRepository;
import com.businessmanager.backend.product.repository.ProductRepository;
import com.businessmanager.backend.product.repository.TaxRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final TaxRateRepository taxRateRepository;
    private final com.businessmanager.backend.customer.repository.CustomerRepository customerRepository;

    @Override
    @Transactional
    @AuditAction(action = "CREATE", module = "PRODUCT")
    public Product createProduct(Product product, Long categoryId, Long taxRateId) {
        if (productRepository.existsBySku(product.getSku())) {
            throw new BusinessRuleException("Product SKU already exists: " + product.getSku());
        }

        if (categoryId != null) {
            ProductCategory cat = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));
            product.setCategory(cat);
        }

        if (taxRateId != null) {
            TaxRate tax = taxRateRepository.findById(taxRateId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tax rate not found with ID: " + taxRateId));
            product.setTaxRate(tax);
        }

        return productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Product getProductBySku(String sku) {
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> searchProducts(String search, Long categoryId, ProductStatus status, Pageable pageable) {
        return productRepository.searchProducts(search, categoryId, status, pageable);
    }

    @Override
    @Transactional
    @AuditAction(action = "UPDATE", module = "PRODUCT")
    public Product updateProduct(Long id, Product updateDetails, Long categoryId, Long taxRateId) {
        Product product = getProductById(id);

        // If SKU changed, verify uniqueness
        if (!product.getSku().equals(updateDetails.getSku()) && productRepository.existsBySku(updateDetails.getSku())) {
            throw new BusinessRuleException("Product SKU already exists: " + updateDetails.getSku());
        }

        product.setSku(updateDetails.getSku());
        product.setName(updateDetails.getName());
        product.setHsnCode(updateDetails.getHsnCode());
        product.setDescription(updateDetails.getDescription());
        product.setUnitOfMeasure(updateDetails.getUnitOfMeasure());
        product.setCostPrice(updateDetails.getCostPrice());
        product.setBaseSellingPrice(updateDetails.getBaseSellingPrice());
        product.setReorderLevel(updateDetails.getReorderLevel());
        product.setReorderQty(updateDetails.getReorderQty());
        product.setBatchTracked(updateDetails.isBatchTracked());
        product.setIncludeInFinancialCalculations(updateDetails.isIncludeInFinancialCalculations());
        product.setStatus(updateDetails.getStatus());

        if (categoryId != null) {
            ProductCategory cat = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));
            product.setCategory(cat);
        } else {
            product.setCategory(null);
        }

        if (taxRateId != null) {
            TaxRate tax = taxRateRepository.findById(taxRateId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tax rate not found with ID: " + taxRateId));
            product.setTaxRate(tax);
        } else {
            product.setTaxRate(null);
        }

        return productRepository.save(product);
    }

    @Override
    @Transactional
    @AuditAction(action = "DEACTIVATE", module = "PRODUCT")
    public void deactivateProduct(Long id) {
        Product product = getProductById(id);
        product.setStatus(ProductStatus.INACTIVE);
        productRepository.save(product);
    }

    @Override
    @Transactional
    @AuditAction(action = "DELETE", module = "PRODUCT")
    public void deleteProduct(Long id) {
        Product product = getProductById(id);

        if (hasTransactionHistory(product)) {
            throw new BusinessRuleException("Product has transaction history and cannot be deleted. Deactivate instead.");
        }

        productRepository.delete(product);
    }

    private boolean hasTransactionHistory(Product product) {
        // Hooks into future transaction module checks.
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    @AuditAction(action = "EXPORT", module = "PRODUCT")
    public String exportProductsToCsv() {
        java.util.List<Product> products = productRepository.findAll();
        StringBuilder csv = new StringBuilder();
        
        // Header
        csv.append("SKU,Name,Category,UoM,CostPrice,SellingPrice,TaxRate,StockOnHand,ReorderLevel,BatchTracked,Status\n");
        
        // Rows
        for (Product p : products) {
            csv.append(escapeCsv(p.getSku())).append(",")
               .append(escapeCsv(p.getName())).append(",")
               .append(p.getCategory() != null ? escapeCsv(p.getCategory().getName()) : "").append(",")
               .append(escapeCsv(p.getUnitOfMeasure())).append(",")
               .append(p.getCostPrice()).append(",")
               .append(p.getBaseSellingPrice()).append(",")
               .append(p.getTaxRate() != null ? escapeCsv(p.getTaxRate().getName()) : "").append(",")
               .append(p.getStockOnHand()).append(",")
               .append(p.getReorderLevel()).append(",")
               .append(p.isBatchTracked()).append(",")
               .append(p.getStatus())
               .append("\n");
        }
        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    @Override
    @Transactional(readOnly = true)
    public java.math.BigDecimal resolvePrice(Long productId, Long customerId) {
        Product product = getProductById(productId);
        
        if (customerId == null) {
            return product.getBaseSellingPrice();
        }

        com.businessmanager.backend.customer.entity.Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        java.math.BigDecimal basePrice = product.getBaseSellingPrice();
        Long tierId = customer.getPriceTierId();

        if (tierId == null) {
            return basePrice;
        }

        // Tier price resolution strategy:
        // Tier 1 -> VIP: 10% Discount
        // Tier 2 -> Wholesale: 20% Discount
        // Tier 3 -> Bulk: 5% Discount
        java.math.BigDecimal discountFactor = java.math.BigDecimal.ONE;
        if (tierId == 1) {
            discountFactor = new java.math.BigDecimal("0.90");
        } else if (tierId == 2) {
            discountFactor = new java.math.BigDecimal("0.80");
        } else if (tierId == 3) {
            discountFactor = new java.math.BigDecimal("0.95");
        }

        return basePrice.multiply(discountFactor).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public java.math.BigDecimal getValuationOfFinancialCalculations() {
        java.math.BigDecimal valuation = productRepository.calculateActiveCatalogValuation();
        return valuation != null ? valuation : java.math.BigDecimal.ZERO;
    }

    @Override
    @Transactional
    @AuditAction(action = "IMPORT", module = "PRODUCT")
    public com.businessmanager.backend.product.dto.ProductImportResultDto importProducts(
            java.util.List<com.businessmanager.backend.product.dto.ProductCreateDto> dtos
    ) {
        java.util.List<com.businessmanager.backend.product.dto.ProductImportResultDto.RowError> errors = new java.util.ArrayList<>();
        java.util.Set<String> processedSkus = new java.util.HashSet<>();
        int successCount = 0;
        int failureCount = 0;

        for (int i = 0; i < dtos.size(); i++) {
            int rowNum = i + 1;
            com.businessmanager.backend.product.dto.ProductCreateDto dto = dtos.get(i);

            // 1. Check SKU empty
            if (dto.getSku() == null || dto.getSku().trim().isEmpty()) {
                errors.add(com.businessmanager.backend.product.dto.ProductImportResultDto.RowError.builder()
                        .rowNumber(rowNum)
                        .sku("")
                        .message("SKU code is required")
                        .build());
                failureCount++;
                continue;
            }

            String sku = dto.getSku().trim();

            // 2. Check SKU duplicate inside same sheet
            if (processedSkus.contains(sku)) {
                errors.add(com.businessmanager.backend.product.dto.ProductImportResultDto.RowError.builder()
                        .rowNumber(rowNum)
                        .sku(sku)
                        .message("Duplicate SKU in import list: " + sku)
                        .build());
                failureCount++;
                continue;
            }

            // 3. Check SKU duplicate in database
            if (productRepository.existsBySku(sku)) {
                errors.add(com.businessmanager.backend.product.dto.ProductImportResultDto.RowError.builder()
                        .rowNumber(rowNum)
                        .sku(sku)
                        .message("Product SKU already exists in database: " + sku)
                        .build());
                failureCount++;
                continue;
            }

            // 4. Check name empty
            if (dto.getName() == null || dto.getName().trim().isEmpty()) {
                errors.add(com.businessmanager.backend.product.dto.ProductImportResultDto.RowError.builder()
                        .rowNumber(rowNum)
                        .sku(sku)
                        .message("Product name is required")
                        .build());
                failureCount++;
                continue;
            }

            // 5. Verify category reference
            ProductCategory category = null;
            if (dto.getCategoryId() != null) {
                java.util.Optional<ProductCategory> catOpt = categoryRepository.findById(dto.getCategoryId());
                if (catOpt.isEmpty()) {
                    errors.add(com.businessmanager.backend.product.dto.ProductImportResultDto.RowError.builder()
                            .rowNumber(rowNum)
                            .sku(sku)
                            .message("Invalid Category ID reference: " + dto.getCategoryId())
                            .build());
                    failureCount++;
                    continue;
                }
                category = catOpt.get();
            }

            // 6. Verify tax rate reference
            TaxRate tax = null;
            if (dto.getTaxRateId() != null) {
                java.util.Optional<TaxRate> taxOpt = taxRateRepository.findById(dto.getTaxRateId());
                if (taxOpt.isEmpty()) {
                    errors.add(com.businessmanager.backend.product.dto.ProductImportResultDto.RowError.builder()
                            .rowNumber(rowNum)
                            .sku(sku)
                            .message("Invalid Tax Rate ID reference: " + dto.getTaxRateId())
                            .build());
                    failureCount++;
                    continue;
                }
                tax = taxOpt.get();
            }

            // Save valid product
            Product product = new Product();
            product.setSku(sku);
            product.setName(dto.getName().trim());
            product.setDescription(dto.getDescription());
            product.setCategory(category);
            product.setUnitOfMeasure(dto.getUnitOfMeasure() != null ? dto.getUnitOfMeasure() : "pcs");
            product.setCostPrice(dto.getCostPrice() != null ? dto.getCostPrice() : java.math.BigDecimal.ZERO);
            product.setBaseSellingPrice(dto.getBaseSellingPrice() != null ? dto.getBaseSellingPrice() : java.math.BigDecimal.ZERO);
            product.setTaxRate(tax);
            product.setReorderLevel(dto.getReorderLevel() != null ? dto.getReorderLevel() : java.math.BigDecimal.ZERO);
            product.setReorderQty(dto.getReorderQty() != null ? dto.getReorderQty() : java.math.BigDecimal.ZERO);
            product.setBatchTracked(dto.isBatchTracked());
            product.setIncludeInFinancialCalculations(dto.isIncludeInFinancialCalculations());
            product.setStatus(ProductStatus.ACTIVE);

            productRepository.save(product);
            processedSkus.add(sku);
            successCount++;
        }

        return com.businessmanager.backend.product.dto.ProductImportResultDto.builder()
                .totalProcessed(dtos.size())
                .successCount(successCount)
                .failureCount(failureCount)
                .errors(errors)
                .build();
    }
}
