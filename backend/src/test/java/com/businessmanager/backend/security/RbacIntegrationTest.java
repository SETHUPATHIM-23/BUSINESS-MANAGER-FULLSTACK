package com.businessmanager.backend.security;

import com.businessmanager.backend.security.config.SecurityConfig;
import com.businessmanager.backend.security.jwt.JwtAuthenticationFilter;
import com.businessmanager.backend.security.jwt.JwtTokenProvider;
import com.businessmanager.backend.security.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RbacIntegrationTest.TestController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
public class RbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtTokenProvider tokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @WithMockUser(authorities = "CUSTOMER_WRITE")
    public void userWithCustomerWrite_CanAccessCustomerWriteEndpoint() throws Exception {
        mockMvc.perform(get("/test/customer-write"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_READ")
    public void userWithoutCustomerWrite_CannotAccessCustomerWriteEndpoint() throws Exception {
        mockMvc.perform(get("/test/customer-write"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_WRITE")
    public void userWithCustomerWrite_CannotAccessAccountingWriteEndpoint() throws Exception {
        mockMvc.perform(get("/test/accounting-write"))
                .andExpect(status().isForbidden());
    }

    @RestController
    static class TestController {
        @GetMapping("/test/customer-write")
        @PreAuthorize("hasAuthority('CUSTOMER_WRITE')")
        public String customerWrite() {
            return "success";
        }

        @GetMapping("/test/accounting-write")
        @PreAuthorize("hasAuthority('ACCOUNTING_WRITE')")
        public String accountingWrite() {
            return "success";
        }
    }
}
