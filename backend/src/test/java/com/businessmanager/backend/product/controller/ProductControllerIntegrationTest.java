package com.businessmanager.backend.product.controller;

import com.businessmanager.backend.common.exception.GlobalExceptionHandler;
import com.businessmanager.backend.product.dto.ProductCreateDto;
import com.businessmanager.backend.product.dto.ProductDetailDto;
import com.businessmanager.backend.product.entity.Product;
import com.businessmanager.backend.product.enums.ProductStatus;
import com.businessmanager.backend.product.mapper.ProductMapper;
import com.businessmanager.backend.product.service.ProductService;
import com.businessmanager.backend.security.config.SecurityConfig;
import com.businessmanager.backend.security.jwt.JwtAuthenticationFilter;
import com.businessmanager.backend.security.jwt.JwtTokenProvider;
import com.businessmanager.backend.security.service.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
public class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private ProductMapper productMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtTokenProvider tokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private ProductCreateDto createDto;
    private ProductDetailDto detailDto;

    @BeforeEach
    void setUp() {
        createDto = new ProductCreateDto();
        createDto.setSku("PROD-100");
        createDto.setName("Aluminum Sheets");
        createDto.setUnitOfMeasure("pcs");
        createDto.setCostPrice(new BigDecimal("45.00"));
        createDto.setBaseSellingPrice(new BigDecimal("90.00"));
        createDto.setReorderLevel(new BigDecimal("10.00"));
        createDto.setReorderQty(new BigDecimal("50.00"));

        detailDto = new ProductDetailDto();
        detailDto.setId(1L);
        detailDto.setSku("PROD-100");
        detailDto.setName("Aluminum Sheets");
        detailDto.setUnitOfMeasure("pcs");
        detailDto.setCostPrice(new BigDecimal("45.00"));
        detailDto.setBaseSellingPrice(new BigDecimal("90.00"));
        detailDto.setStatus(ProductStatus.ACTIVE.name());
    }

    @Test
    @WithMockUser(authorities = {"PRODUCT_WRITE"})
    void createProduct_Success() throws Exception {
        Product mockProduct = new Product();
        when(productMapper.toEntity(any(ProductCreateDto.class))).thenReturn(mockProduct);
        when(productService.createProduct(any(Product.class), any(), any())).thenReturn(mockProduct);
        when(productMapper.toDetailDto(any(Product.class))).thenReturn(detailDto);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("PROD-100"))
                .andExpect(jsonPath("$.name").value("Aluminum Sheets"));
    }

    @Test
    @WithMockUser(authorities = {"PRODUCT_READ"})
    void createProduct_ForbiddenForReadRole() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"PRODUCT_WRITE"})
    void createProduct_ValidationError_ReturnsBadRequest() throws Exception {
        createDto.setName(""); // Invalid name

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"PRODUCT_READ"})
    void resolvePrice_Success() throws Exception {
        when(productService.resolvePrice(1L, 10L)).thenReturn(new BigDecimal("81.00"));

        mockMvc.perform(get("/api/products/1/resolve-price")
                        .param("customerId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(81.00));
    }

    @Test
    @WithMockUser(authorities = {"PRODUCT_READ"})
    void getValuation_Success() throws Exception {
        when(productService.getValuationOfFinancialCalculations()).thenReturn(new BigDecimal("5000.00"));

        mockMvc.perform(get("/api/products/valuation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(5000.00));
    }
}
