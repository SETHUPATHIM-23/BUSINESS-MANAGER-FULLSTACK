package com.businessmanager.backend.product.service;

import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.customer.entity.Customer;
import com.businessmanager.backend.customer.repository.CustomerRepository;
import com.businessmanager.backend.product.entity.Product;
import com.businessmanager.backend.product.entity.ProductCategory;
import com.businessmanager.backend.product.entity.TaxRate;
import com.businessmanager.backend.product.enums.ProductStatus;
import com.businessmanager.backend.product.repository.ProductCategoryRepository;
import com.businessmanager.backend.product.repository.ProductRepository;
import com.businessmanager.backend.product.repository.TaxRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCategoryRepository categoryRepository;

    @Mock
    private TaxRateRepository taxRateRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private ProductCategory category;
    private TaxRate taxRate;

    @BeforeEach
    void setUp() {
        category = new ProductCategory();
        category.setId(1L);
        category.setName("Raw Materials");

        taxRate = new TaxRate();
        taxRate.setId(1L);
        taxRate.setName("Standard VAT");
        taxRate.setRate(new BigDecimal("15.00"));

        product = new Product();
        product.setId(1L);
        product.setSku("PROD-001");
        product.setName("Stainless Steel Sheet");
        product.setUnitOfMeasure("pcs");
        product.setCostPrice(new BigDecimal("50.00"));
        product.setBaseSellingPrice(new BigDecimal("100.00"));
        product.setCategory(category);
        product.setTaxRate(taxRate);
        product.setStatus(ProductStatus.ACTIVE);
    }

    @Test
    void createProduct_Success() {
        when(productRepository.existsBySku("PROD-001")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(taxRateRepository.findById(1L)).thenReturn(Optional.of(taxRate));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product created = productService.createProduct(product, 1L, 1L);

        assertNotNull(created);
        assertEquals("PROD-001", created.getSku());
        verify(productRepository).save(product);
    }

    @Test
    void createProduct_DuplicateSku_ThrowsBusinessRuleException() {
        when(productRepository.existsBySku("PROD-001")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> 
                productService.createProduct(product, 1L, 1L)
        );

        verify(productRepository, never()).save(any());
    }

    @Test
    void getProductById_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Product found = productService.getProductById(1L);

        assertNotNull(found);
        assertEquals(1L, found.getId());
    }

    @Test
    void getProductById_NotFound_ThrowsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
                productService.getProductById(99L)
        );
    }

    @Test
    void updateProduct_Success() {
        Product updateDetails = new Product();
        updateDetails.setSku("PROD-001"); // same SKU
        updateDetails.setName("Steel Sheet V2");
        updateDetails.setUnitOfMeasure("pcs");
        updateDetails.setCostPrice(new BigDecimal("60.00"));
        updateDetails.setBaseSellingPrice(new BigDecimal("110.00"));
        updateDetails.setStatus(ProductStatus.ACTIVE);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(taxRateRepository.findById(1L)).thenReturn(Optional.of(taxRate));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product updated = productService.updateProduct(1L, updateDetails, 1L, 1L);

        assertEquals("Steel Sheet V2", updated.getName());
        assertEquals(new BigDecimal("110.00"), updated.getBaseSellingPrice());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_DuplicateSku_ThrowsBusinessRuleException() {
        Product updateDetails = new Product();
        updateDetails.setSku("PROD-002"); // new SKU
        updateDetails.setName("Steel Sheet");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySku("PROD-002")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> 
                productService.updateProduct(1L, updateDetails, 1L, 1L)
        );

        verify(productRepository, never()).save(any());
    }

    @Test
    void deactivateProduct_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.deactivateProduct(1L);

        assertEquals(ProductStatus.INACTIVE, product.getStatus());
        verify(productRepository).save(product);
    }

    @Test
    void deleteProduct_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        doNothing().when(productRepository).delete(product);

        productService.deleteProduct(1L);

        verify(productRepository).delete(product);
    }

    @Test
    void resolvePrice_NoCustomer_ReturnsBasePrice() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        BigDecimal resolved = productService.resolvePrice(1L, null);

        assertEquals(new BigDecimal("100.00"), resolved);
    }

    @Test
    void resolvePrice_NoPriceTier_ReturnsBasePrice() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setPriceTierId(null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        BigDecimal resolved = productService.resolvePrice(1L, 1L);

        assertEquals(new BigDecimal("100.00"), resolved);
    }

    @Test
    void resolvePrice_Tier1VIP_ReturnsTenPercentDiscount() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setPriceTierId(1L); // VIP

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        BigDecimal resolved = productService.resolvePrice(1L, 1L);

        assertEquals(new BigDecimal("90.00"), resolved); // 100 * 0.90
    }

    @Test
    void resolvePrice_Tier2Wholesale_ReturnsTwentyPercentDiscount() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setPriceTierId(2L); // Wholesale

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        BigDecimal resolved = productService.resolvePrice(1L, 1L);

        assertEquals(new BigDecimal("80.00"), resolved); // 100 * 0.80
    }

    @Test
    void resolvePrice_Tier3Bulk_ReturnsFivePercentDiscount() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setPriceTierId(3L); // Bulk

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        BigDecimal resolved = productService.resolvePrice(1L, 1L);

        assertEquals(new BigDecimal("95.00"), resolved); // 100 * 0.95
    }

    @Test
    void getValuationOfFinancialCalculations_Success() {
        when(productRepository.calculateActiveCatalogValuation()).thenReturn(new BigDecimal("250.00"));

        BigDecimal valuation = productService.getValuationOfFinancialCalculations();

        assertEquals(new BigDecimal("250.00"), valuation);
    }

    @Test
    void getValuationOfFinancialCalculations_NullValuation_ReturnsZero() {
        when(productRepository.calculateActiveCatalogValuation()).thenReturn(null);

        BigDecimal valuation = productService.getValuationOfFinancialCalculations();

        assertEquals(BigDecimal.ZERO, valuation);
    }
}
