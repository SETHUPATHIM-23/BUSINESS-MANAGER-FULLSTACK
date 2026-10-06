package com.businessmanager.backend.settings.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSetupRequest {

    // Administrator Account
    private String adminUsername;
    private String adminPassword;

    // Company Information
    private String companyName;

    private String legalName;
    private String address;
    private String city;
    private String state;
    private String stateCode;
    private String pincode;
    private String country;
    private String mobile;
    private String telephone;
    private String email;
    private String website;
    private String gstin;
    private String pan;

    // Business & Invoice Configuration
    private String invoicePrefix;
    private Long invoiceNextNumber;
    private String currencySymbol;
    private String currencyCode;
    private String invoiceTerms;
    private String invoiceFooter;

    // JSON Extensions
    private String vehiclesJson;
    private String bankDetailsJson;
}
