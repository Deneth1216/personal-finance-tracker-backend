package com.financetracker.financetrackerbackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException; // Spring Security's base authentication exception
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Handles specific exceptions thrown by our AuthService (e.g., user already exists)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
            new Date(),
            ex.getMessage(), // Message from our AuthService
            request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    // Handles authentication failures (e.g., bad credentials, user not found via UserDetailsService)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
            new Date(),
            "Authentication failed: " + ex.getMessage(), // Can be more generic if needed
            request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(errorDetails, HttpStatus.UNAUTHORIZED);
    }

    // A general handler for other exceptions (e.g., NullPointerException, etc.)
    // This acts as a fallback.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(Exception ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
            new Date(),
            "An unexpected error occurred: " + ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        // Log the exception here for debugging
        // ex.printStackTrace(); // Or use a logger
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Simple ErrorDetails class (can be an inner class or a separate DTO)
    public static class ErrorDetails {
        private Date timestamp;
        private String message;
        private String path;

        public ErrorDetails(Date timestamp, String message, String path) {
            this.timestamp = timestamp;
            this.message = message;
            this.path = path;
        }

        // Getters
        public Date getTimestamp() { return timestamp; }
        public String getMessage() { return message; }
        public String getPath() { return path; }
    }
}
