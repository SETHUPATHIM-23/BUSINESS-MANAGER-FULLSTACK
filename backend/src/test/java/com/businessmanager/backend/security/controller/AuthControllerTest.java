package com.businessmanager.backend.security.controller;

import com.businessmanager.backend.security.config.SecurityConfig;
import com.businessmanager.backend.security.dto.LoginRequest;
import com.businessmanager.backend.security.jwt.JwtAuthenticationFilter;
import com.businessmanager.backend.security.jwt.JwtTokenProvider;
import com.businessmanager.backend.security.service.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtTokenProvider tokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private com.businessmanager.backend.security.service.UserService userService;

    @MockBean
    private com.businessmanager.backend.security.service.AuthService authService;

    @Test
    public void loginWithValidCredentials_ReturnsTokens() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("adminpassword");

        UserDetails userDetails = User.withUsername("admin")
                .password("hashed_password")
                .authorities("ROLE_ADMINISTRATOR")
                .build();

        UsernamePasswordAuthenticationToken authResult = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        com.businessmanager.backend.security.entity.User dbUser = new com.businessmanager.backend.security.entity.User();
        dbUser.setUsername("admin");
        com.businessmanager.backend.security.entity.Role role = new com.businessmanager.backend.security.entity.Role();
        role.setName("ROLE_ADMINISTRATOR");
        dbUser.setRoles(java.util.Set.of(role));

        when(userService.getUserByUsername("admin")).thenReturn(dbUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authResult);
        when(tokenProvider.generateAccessToken(any(UserDetails.class))).thenReturn("mockAccessJwt");
        when(tokenProvider.generateRefreshToken(any(UserDetails.class))).thenReturn("mockRefreshJwt");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mockAccessJwt"))
                .andExpect(jsonPath("$.refreshToken").value("mockRefreshJwt"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMINISTRATOR"));
    }

    @Test
    public void loginWithInvalidCredentials_ReturnsUnauthorized() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void callingProtectedEndpointWithoutToken_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/any-protected-endpoint"))
                .andExpect(status().isForbidden());
    }
}
