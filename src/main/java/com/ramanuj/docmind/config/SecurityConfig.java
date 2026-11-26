
package com.ramanuj.docmind.config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ramanuj.docmind.entity.Role;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    private final JwtAuthenticationFilter authenticationFilter;
    private final AppProperties appProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()).cors(Customizer.withDefaults()).sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(auth -> auth

                        // Allow CORS preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Allow async and internal error dispatches
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()

                        // Public endpoints
                        .requestMatchers("/api/v1/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Admin endpoints
                        .requestMatchers("/api/v1/admin/**").hasRole(Role.ADMIN.name())

                        // Authenticated user endpoints
                        .requestMatchers("/api/v1/**").hasAnyRole(Role.USER.name(), Role.ADMIN.name()).anyRequest().authenticated())
                .exceptionHandling(exception -> exception.authenticationEntryPoint((request, response, authException) -> {
                            logger.debug("Authentication failed for {}: {}", request.getRequestURI(), authException.getMessage());
                            writeError(response, HttpStatus.UNAUTHORIZED, "Authentication is required");
                        }).accessDeniedHandler((request, response, accessException) -> {
                            logger.warn("Access denied for {}: {}", request.getRequestURI(), accessException.getMessage());
                            writeError(response, HttpStatus.FORBIDDEN, "You do not have permission to access this resource");
                        })).addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Writes a consistent JSON error response.
     */
    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String timestamp = LocalDateTime.now().toString();
        response.getWriter().write("{" + "\"success\":false," + "\"message\":\"" + message + "\"," + "\"data\":null,"
                        + "\"timestamp\":\"" + timestamp + "\"" + "}");
        response.getWriter().flush();
    }

    /**
     * CORS configuration.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(split(appProperties.getCors().getAllowedOrigin()));
        configuration.setAllowedMethods(split(appProperties.getCors().getAllowedMethods()));
        configuration.setAllowedHeaders(split(appProperties.getCors().getAllowedHeaders()));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Converts comma-separated values into a list.
     */
    private List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> result = new java.util.ArrayList<>();
        for (String item : value.split(",")) {
            String trimmedItem = item.trim();
            if (!trimmedItem.isEmpty()) {
                result.add(trimmedItem);
            }
        }
        return result;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }
}