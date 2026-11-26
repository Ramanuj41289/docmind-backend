
package com.ramanuj.docmind.config;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ramanuj.docmind.entity.Role;
import com.ramanuj.docmind.entity.User;
import com.ramanuj.docmind.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.username:}")
    private String username;

    @Value("${app.bootstrap-admin.email:}")
    private String email;

    @Value("${app.bootstrap-admin.password:}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        boolean usernameProvided = username != null && !username.isBlank();
        boolean emailProvided = email != null && !email.isBlank();
        boolean passwordProvided = password != null && !password.isBlank();
        // Bootstrap is optional. If no credentials are configured, skip it.
        if (!usernameProvided && !emailProvided && !passwordProvided) {
            logger.info("Admin bootstrap is not configured. Skipping.");
            return;
        }

        // Reject incomplete configuration.
        if (!usernameProvided || !emailProvided || !passwordProvided) {
            throw new IllegalStateException("Admin bootstrap requires username, email and password");
        }

        String normalizedUsername = username.trim();
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (password.length() < 12) {
            throw new IllegalStateException(
                    "Initial admin password must contain at least 12 characters");
        }

        var existingByUsername =
                userRepository.findByUsername(normalizedUsername);

        if (existingByUsername.isPresent()) {
            User existingUser = existingByUsername.get();

            if (existingUser.getRole() == Role.ADMIN
                    && existingUser.getEmail().equalsIgnoreCase(normalizedEmail)) {
                logger.info("Initial admin account already exists. Skipping creation.");
                return;
            }

            throw new IllegalStateException(
                    "Bootstrap username already belongs to an existing account. "
                    + "No role or account details were changed.");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException(
                    "Bootstrap email already belongs to another account. "
                    + "No account was created.");
        }

        User admin = new User();
        admin.setUsername(normalizedUsername);
        admin.setEmail(normalizedEmail);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);

        userRepository.save(admin);

        logger.info("Initial administrator account created successfully.");
    }
}
