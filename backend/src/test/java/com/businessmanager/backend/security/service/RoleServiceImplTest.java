package com.businessmanager.backend.security.service;

import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.security.entity.Role;
import com.businessmanager.backend.security.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role role;

    @BeforeEach
    void setUp() {
        role = new Role();
        role.setId(1L);
        role.setName("ROLE_MANAGER");
    }

    @Test
    void createRole_Success() {
        when(roleRepository.existsByName("ROLE_MANAGER")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        Role created = roleService.createRole(role);

        assertNotNull(created);
        verify(roleRepository).save(role);
    }

    @Test
    void createRole_DuplicateName_ThrowsException() {
        when(roleRepository.existsByName("ROLE_MANAGER")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> roleService.createRole(role));
        verify(roleRepository, never()).save(any());
    }

    @Test
    void updateRole_Success() {
        Role updated = new Role();
        updated.setName("ROLE_SUPERVISOR");

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.existsByName("ROLE_SUPERVISOR")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        Role result = roleService.updateRole(1L, updated);

        assertNotNull(result);
        assertEquals("ROLE_SUPERVISOR", role.getName());
    }

    @Test
    void updateRole_DuplicateName_ThrowsException() {
        Role updated = new Role();
        updated.setName("ROLE_EXISTING");

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.existsByName("ROLE_EXISTING")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> roleService.updateRole(1L, updated));
        verify(roleRepository, never()).save(any());
    }

    @Test
    void updateRole_RenameAdmin_ThrowsException() {
        role.setName("ROLE_ADMINISTRATOR");
        Role updated = new Role();
        updated.setName("ROLE_SUPER_ADMIN");

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.existsByName("ROLE_SUPER_ADMIN")).thenReturn(false);

        assertThrows(BusinessRuleException.class, () -> roleService.updateRole(1L, updated));
        verify(roleRepository, never()).save(any());
    }

    @Test
    void deactivateRole_Admin_ThrowsException() {
        role.setName("ROLE_ADMINISTRATOR");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        assertThrows(BusinessRuleException.class, () -> roleService.deactivateRole(1L));
    }

    @Test
    void deactivateRole_StandardRole_ThrowsException() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        assertThrows(BusinessRuleException.class, () -> roleService.deactivateRole(1L));
    }
}
