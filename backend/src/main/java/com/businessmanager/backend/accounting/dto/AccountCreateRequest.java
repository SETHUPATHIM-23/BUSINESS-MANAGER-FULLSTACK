package com.businessmanager.backend.accounting.dto;

import com.businessmanager.backend.accounting.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountCreateRequest {

    @NotBlank(message = "Account code is required")
    @Size(max = 20, message = "Account code cannot exceed 20 characters")
    private String code;

    @NotBlank(message = "Account name is required")
    @Size(max = 100, message = "Account name cannot exceed 100 characters")
    private String name;

    @NotNull(message = "Account type is required")
    private AccountType type;

    private Long parentId;

    private boolean active = true;
}
