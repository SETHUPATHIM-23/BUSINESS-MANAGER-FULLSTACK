package com.businessmanager.backend.settings.dto;

import com.businessmanager.backend.settings.enums.InstallationState;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatusResponse {
    private InstallationState installationState;
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
    private String invoicePrefix;
    private Long invoiceNextNumber;
    private String currencySymbol;
    private String currencyCode;
    private String invoiceTerms;
    private String invoiceFooter;
    private String vehiclesJson;
    private String bankDetailsJson;
}
