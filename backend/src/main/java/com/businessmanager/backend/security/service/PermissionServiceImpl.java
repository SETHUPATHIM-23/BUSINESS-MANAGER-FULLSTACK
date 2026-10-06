package com.businessmanager.backend.security.service;

import com.businessmanager.backend.security.entity.Permission;
import com.businessmanager.backend.security.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Permission> searchPermissions(String search, Pageable pageable) {
        return permissionRepository.searchPermissions(search, pageable);
    }
}
