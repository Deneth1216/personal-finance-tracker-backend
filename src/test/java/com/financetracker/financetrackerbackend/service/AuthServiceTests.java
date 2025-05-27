package com.financetracker.financetrackerbackend.service;

import com.financetracker.financetrackerbackend.dto.RegistrationRequest;
import com.financetracker.financetrackerbackend.entity.User; // Ensure this is the correct User entity
import com.financetracker.financetrackerbackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void testRegisterUser_Success() {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        
        // This User object represents the state of the user *as it would be passed to the save method*.
        // The AuthService is responsible for setting the username, email (from request),
        // encoded password, default roles, and enabled status *before* calling save.
        User userAsSaved = new User(); 
        userAsSaved.setUsername(request.getUsername());
        userAsSaved.setEmail(request.getEmail());
        userAsSaved.setPassword("hashedPassword"); // Password already encoded by the service
        userAsSaved.setRoles("ROLE_USER");         // Default role set by the service
        userAsSaved.setEnabled(true);              // Default enabled status set by the service

        // Mock the save operation. We expect a User object (any User instance) to be passed.
        // The `thenAnswer` part simulates that the save operation might return an instance
        // that could have database-generated fields (like an ID), but for this unit test,
        // returning the captured argument (or a conceptually similar object) is fine.
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User userPassedToSave = invocation.getArgument(0);
            // For the purpose of this unit test, we can assume the saved user returned
            // is the same as the one passed, or a new one with the same relevant fields.
            // If an ID were generated and used later *within the registerUser method*, 
            // we'd need to mock that (e.g., userPassedToSave.setId(1L);).
            // Since it's not, just returning it is sufficient.
            return userPassedToSave; 
        });

        User result = authService.registerUser(request);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("hashedPassword", result.getPassword());
        assertEquals("ROLE_USER", result.getRoles());
        assertTrue(result.isEnabled());

        // Verify that save was called, and we can use an ArgumentCaptor to check the details
        // of the object passed to save if needed, though the assertions on 'result' cover most of it.
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegisterUser_UsernameAlreadyExists() {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("existinguser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.registerUser(request);
        });
        assertEquals("Error: Username is already taken!", exception.getMessage());
        verify(userRepository, never()).save(any(User.class)); // Ensure save is not called
    }

    @Test
    void testRegisterUser_EmailAlreadyExists() {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("newuser");
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("newuser")).thenReturn(false); // Username is not taken
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true); // Email is taken

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.registerUser(request);
        });
        assertEquals("Error: Email is already in use!", exception.getMessage());
        verify(userRepository, never()).save(any(User.class)); // Ensure save is not called
    }
}
