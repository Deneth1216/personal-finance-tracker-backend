package com.financetracker.financetrackerbackend.config;

import com.financetracker.financetrackerbackend.service.UserDetailsServiceImpl; // Assuming this is your UserDetailsService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity // Enables Spring Security's web security support
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;

    @Autowired
    public SecurityConfig(UserDetailsServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
            http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(userDetailsService)
            .passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // For stateless APIs or if using custom CSRF handling. Enable for session-based.
                                         // For this setup with a /api/auth/login, disabling simplifies things.
                                         // If keeping sessions, .csrf(withDefaults()) might be better.
            .authorizeHttpRequests(authorizeRequests ->
                authorizeRequests
                    .requestMatchers("/api/auth/register", "/api/auth/login").permitAll() // Public endpoints
                    .requestMatchers("/error").permitAll() // Allow error pages
                    // Add other public paths like H2 console if used: .requestMatchers("/h2-console/**").permitAll()
                    .anyRequest().authenticated() // All other requests need authentication
            )
            .formLogin(formLogin -> // This is for browser-based login form, may not be directly used by SPA if /api/auth/login is primary
                formLogin
                    .loginPage("/login") // If you have a custom login page served by Spring MVC
                    .permitAll()
            )
            .logout(logout -> // Configures logout behavior
                logout
                    .logoutUrl("/api/auth/logout") // Define a logout URL
                    .permitAll()
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID") // If using session cookies
            );
            // .httpBasic(withDefaults()); // Could enable HTTP Basic auth if needed

        // If using H2 console, you might need to allow frames:
        // http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        return http.build();
    }
}
