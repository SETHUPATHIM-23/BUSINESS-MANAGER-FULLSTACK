package com.businessmanager.backend.settings.service;

import com.businessmanager.backend.settings.dto.SystemSetupRequest;
import com.businessmanager.backend.settings.dto.SystemStatusResponse;

public interface SystemSettingsService {
    SystemStatusResponse getSystemStatus();
    SystemStatusResponse performFirstTimeSetup(SystemSetupRequest request);
    SystemStatusResponse updateCompanySettings(SystemSetupRequest request);
}
