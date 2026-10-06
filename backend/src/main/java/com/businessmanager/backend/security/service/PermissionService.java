package com.businessmanager.backend.security.service;

import com.businessmanager.backend.security.entity.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PermissionService {
    Page<Permission> searchPermissions(String search, Pageable pageable);
}
