package com.businessmanager.backend.product.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProductImportResultDto {
    private int totalProcessed;
    private int successCount;
    private int failureCount;
    private List<RowError> errors;

    @Data
    @Builder
    public static class RowError {
        private int rowNumber;
        private String sku;
        private String message;
    }
}
