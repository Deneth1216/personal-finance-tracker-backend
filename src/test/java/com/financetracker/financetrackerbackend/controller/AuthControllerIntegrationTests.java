package com.financetracker.financetrackerbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financetracker.financetrackerbackend.dto.LoginRequest;
import com.financetracker.financetrackerbackend.dto.RegistrationRequest;
import com.financetracker.financetrackerbackend.entity.User;
import com.financetracker.financetrackerbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.test.context.support.WithMockUser; // Added for @WithMockUser

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get; // Added for GET requests
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Rollback database changes after each test
public class AuthControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll(); // Clean slate before each test
        // Security context is also cleared implicitly by Spring Test framework for new test methods.

        // Setup a common user for tests that need an existing user
        testUser = new User();
        testUser.setUsername("existinguser");
        testUser.setEmail("existing@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setRoles("ROLE_USER");
        testUser.setEnabled(true);
    }

    private User saveTestUser() {
        return userRepository.save(testUser);
    }

    @Test
    void testRegisterUser_Success() throws Exception {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(content().string("User registered successfully! Username: testuser"));

        var userOp = userRepository.findByUsername("testuser");
        assertTrue(userOp.isPresent(), "User should be saved in the database");
        User savedUser = userOp.get();
        assertNotNull(savedUser.getId(), "User ID should not be null");
        assertTrue(passwordEncoder.matches("password123", savedUser.getPassword()), "Password should be hashed correctly");
        assertEquals("ROLE_USER", savedUser.getRoles(), "Default role should be ROLE_USER");
        assertTrue(savedUser.isEnabled(), "User should be enabled by default");
    }

    @Test
    void testRegisterUser_UsernameAlreadyExists() throws Exception {
        saveTestUser(); // Save the initial user

        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("existinguser"); // Same username
        request.setEmail("newemail@example.com");
        request.setPassword("newpassword123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("Error: Username is already taken!")));
    }

    @Test
    void testRegisterUser_EmailAlreadyExists() throws Exception {
        saveTestUser(); // Save the initial user

        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("newuser");
        request.setEmail("existing@example.com"); // Same email
        request.setPassword("newpassword123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("Error: Email is already in use!")));
    }

    @Test
    void testLoginUser_Success() throws Exception {
        saveTestUser(); // Ensure user exists

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("existinguser");
        loginRequest.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("User logged in successfully. Welcome existinguser!")))
            .andReturn();

        // Check that the security context contains an authenticated principal
        assertNotNull(SecurityContextHolder.getContext().getAuthentication(), "Authentication should not be null in SecurityContextHolder");
        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated(), "User should be authenticated");
        assertEquals("existinguser", SecurityContextHolder.getContext().getAuthentication().getName(), "Authenticated principal name should match");
    }

    @Test
    void testLoginUser_BadCredentials_WrongPassword() throws Exception {
        saveTestUser(); // Ensure user exists

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("existinguser");
        loginRequest.setPassword("wrongpassword"); // Incorrect password

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized()) // GlobalExceptionHandler maps AuthenticationException to 401
            .andExpect(jsonPath("$.message", containsString("Authentication failed: Bad credentials")));
            // Spring Security's default message for bad credentials is "Bad credentials"
    }

    @Test
    void testLoginUser_BadCredentials_UserNotFound() throws Exception {
        // Do NOT save any user, or ensure 'nonexistentuser' is not in DB
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("nonexistentuser");
        loginRequest.setPassword("anypassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized()) // GlobalExceptionHandler maps AuthenticationException to 401
            .andExpect(jsonPath("$.message", containsString("Authentication failed: User not found with username: nonexistentuser")));
            // This message comes from our UserDetailsServiceImpl
    }

    // New tests for endpoint security

    @Test
    void testAccessProtectedEndpoint_WithoutAuthentication_Denied() throws Exception {
        mockMvc.perform(get("/api/auth/hello"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testmockuser", roles = {"USER"}) // Simplifies testing secured endpoint
    void testAccessProtectedEndpoint_WithMockUser_Allowed() throws Exception {
        mockMvc.perform(get("/api/auth/hello"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Hello, testmockuser!")));
    }

    @Test
    void testAccessProtectedEndpoint_WithFullLogin_Allowed() throws Exception {
        // 1. Register and save user (or directly save if registration logic is complex/tested elsewhere)
        // For this test, directly saving is cleaner as registration itself isn't the focus.
        User userToLogin = new User();
        userToLogin.setUsername("fullloginuser");
        userToLogin.setEmail("fulllogin@example.com");
        userToLogin.setPassword(passwordEncoder.encode("password123"));
        userToLogin.setRoles("ROLE_USER");
        userToLogin.setEnabled(true);
        userRepository.save(userToLogin);

        // 2. Login user
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("fullloginuser");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk()); // Ensure login is successful

        // MockMvc automatically maintains the session for subsequent requests in the same test.

        // 3. Access protected endpoint
        mockMvc.perform(get("/api/auth/hello"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Hello, fullloginuser!")));
    }

    @Test
    void testErrorEndpoint_Accessible() throws Exception {
        // This test checks that /error is not forbidden by security rules.
        // The actual status code for a direct GET to /error without a prior error
        // can vary (e.g., 500 or 404). Spring Boot's BasicErrorController typically handles it.
        // We expect it not to be 401 or 403.
        // A 500 is a common response if no specific error attributes are set.
        mockMvc.perform(get("/error"))
            .andExpect(status().isInternalServerError());
    }
}
