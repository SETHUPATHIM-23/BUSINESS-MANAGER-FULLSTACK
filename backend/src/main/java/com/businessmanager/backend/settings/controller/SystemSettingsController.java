package com.businessmanager.backend.settings.controller;

import com.businessmanager.backend.settings.dto.SystemSetupRequest;
import com.businessmanager.backend.settings.dto.SystemStatusResponse;
import com.businessmanager.backend.settings.service.SystemSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class SystemSettingsController {

    private final SystemSettingsService settingsService;

    @GetMapping({"/api/setup/status", "/api/v1/setup/status"})
    public ResponseEntity<SystemStatusResponse> getStatus() {
        return ResponseEntity.ok(settingsService.getSystemStatus());
    }

    @PostMapping({"/api/setup/initialize", "/api/v1/setup/initialize"})
    public ResponseEntity<SystemStatusResponse> initializeSystem(@Valid @RequestBody SystemSetupRequest request) {
        return ResponseEntity.ok(settingsService.performFirstTimeSetup(request));
    }

    @GetMapping({"/api/settings/company", "/api/v1/settings/company"})
    public ResponseEntity<SystemStatusResponse> getCompanySettings() {
        return ResponseEntity.ok(settingsService.getSystemStatus());
    }

    @PutMapping({"/api/settings/company", "/api/v1/settings/company"})
    public ResponseEntity<SystemStatusResponse> updateCompanySettings(@Valid @RequestBody SystemSetupRequest request) {
        return ResponseEntity.ok(settingsService.updateCompanySettings(request));
    }
}
