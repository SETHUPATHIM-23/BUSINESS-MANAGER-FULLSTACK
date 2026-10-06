package com.businessmanager.backend.security.controller;

import com.businessmanager.backend.security.dto.LoginRequest;
import com.businessmanager.backend.security.dto.UserCreateRequest;
import com.businessmanager.backend.security.entity.User;
import com.businessmanager.backend.security.enums.UserStatus;
import com.businessmanager.backend.security.repository.AuditLogEntryRepository;
import com.businessmanager.backend.security.repository.PermissionRepository;
import com.businessmanager.backend.security.repository.RoleRepository;
import com.businessmanager.backend.security.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RoleRepository roleRepository;

    @MockBean
    private PermissionRepository permissionRepository;

    @MockBean
    private AuditLogEntryRepository auditLogEntryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("jdoe");
        testUser.setPasswordHash(passwordEncoder.encode("SecurePass1!"));
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setFailedLoginCount(0);
    }

    // ── AUTHORIZATION GATING TESTS ─────────────────────────────────────

    @Test
    @WithMockUser(authorities = "CUSTOMER_READ")
    public void getUsers_NoPermission_Forbidden() throws Exception {
        mockMvc.perform(get("/api/admins/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_READ")
    public void getUsers_HasPermission_Ok() throws Exception {
        when(userRepository.searchUsers(any(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.singletonList(testUser)));

        mockMvc.perform(get("/api/admins/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_READ")
    public void createUser_ReadOnlyUser_Forbidden() throws Exception {
        UserCreateRequest req = new UserCreateRequest();
        req.setUsername("newuser");
        req.setPassword("Valid1@Pass");

        mockMvc.perform(post("/api/admins/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_WRITE")
    public void createUser_WriteUser_Ok() throws Exception {
        UserCreateRequest req = new UserCreateRequest();
        req.setUsername("newuser");
        req.setPassword("Valid1@Pass");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        mockMvc.perform(post("/api/admins/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    // ── VALIDATION ERROR TESTS ──────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "SYSTEM_WRITE")
    public void createUser_InvalidPasswordPolicy_BadRequest() throws Exception {
        UserCreateRequest req = new UserCreateRequest();
        req.setUsername("newuser");
        req.setPassword("weakpass"); // Fails password policy

        mockMvc.perform(post("/api/admins/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password must contain at least one uppercase letter"));
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_WRITE")
    public void createUser_InvalidUsername_BadRequest() throws Exception {
        UserCreateRequest req = new UserCreateRequest();
        req.setUsername("a"); // Too short
        req.setPassword("Valid1@Pass");

        mockMvc.perform(post("/api/admins/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    // ── END-TO-END LOCKOUT FLOW VIA API ─────────────────────────────

    @Test
    public void login_MultipleFailures_LocksAccount() throws Exception {
        // Mock the user repo to always return the same user instance for tracking failed attempts
        when(userRepository.findByUsername("jdoe")).thenReturn(Optional.of(testUser));
        
        // Ensure authentication manager throws BadCredentialsException
        // We'll pass the wrong password via the API payload
        LoginRequest req = new LoginRequest();
        req.setUsername("jdoe");
        req.setPassword("WrongPassword!");

        // 1st attempt
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        // 2nd attempt
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        // 3rd attempt
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        // 4th attempt
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        // 5th attempt - Should lock the account
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        // Verify that the user status is now locked
        assert(testUser.getStatus() == UserStatus.LOCKED);
        assert(testUser.getFailedLoginCount() == 5);
        assert(testUser.getLockedUntil() != null);
        
        // Verify save was called 5 times
        verify(userRepository, times(5)).save(testUser);
    }
}
