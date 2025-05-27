package com.financetracker.financetrackerbackend.controller;

import com.financetracker.financetrackerbackend.dto.LoginRequest;
import com.financetracker.financetrackerbackend.dto.RegistrationRequest;
import com.financetracker.financetrackerbackend.entity.User;
import com.financetracker.financetrackerbackend.service.AuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping; // Add this import

import jakarta.validation.Valid; // Placeholder for when validation is added

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager; // Will be automatically available

    @Autowired
    public AuthController(AuthService authService, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegistrationRequest registrationRequest) {
        // Add @Valid later if DTO has validation annotations
        try {
            User registeredUser = authService.registerUser(registrationRequest);
            // Avoid returning the password in the response.
            // Consider creating a UserResponse DTO without sensitive fields.
            // For now, returning a simple message.
            return ResponseEntity.ok("User registered successfully! Username: " + registeredUser.getUsername());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        // Add @Valid later if DTO has validation annotations
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        // UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        // String username = userDetails.getUsername();
        // In a real app, you'd typically generate a JWT here and return it.
        // For now, a simple success message.
        return ResponseEntity.ok("User logged in successfully. Welcome " + authentication.getName() + "!");
    }

    // A simple /logout endpoint can be configured via SecurityFilterChain more easily.
    // If custom logout logic is needed (e.g., invalidating a token), it can be added here.
    // For session-based auth, Spring Security's logout handler is usually sufficient.

    @GetMapping("/hello")
    public ResponseEntity<String> helloAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Spring Security will ensure authentication is not null and is authenticated
        // if the endpoint is correctly secured.
        return ResponseEntity.ok("Hello, " + authentication.getName() + "!");
    }
}
