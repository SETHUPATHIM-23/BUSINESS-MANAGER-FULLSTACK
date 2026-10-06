package com.businessmanager.backend.supplier.dto;

import com.businessmanager.backend.supplier.enums.SupplierStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupplierUpdateDto {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 100, message = "Business name must not exceed 100 characters")
    private String businessName;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @Size(max = 50, message = "Tax ID must not exceed 50 characters")
    private String taxId;

    @Min(value = 0, message = "Payment terms must be 0 or more days")
    private int paymentTermsDays;

    @Size(max = 500, message = "Bank account details must not exceed 500 characters")
    private String bankAccountDetails;

    @NotNull(message = "Status is required")
    private SupplierStatus status;
}
