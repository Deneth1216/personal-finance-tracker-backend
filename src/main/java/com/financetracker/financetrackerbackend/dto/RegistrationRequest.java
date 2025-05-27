package com.financetracker.financetrackerbackend.dto;

// Add validation annotations later if desired (e.g., @NotBlank, @Size, @Email)
public class RegistrationRequest {
    private String username;
    private String email;
    private String password;
    // Roles could be set by default in the service or passed in request
    // For simplicity, we'll set a default role in the service for now.

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
