package com.springrag.security;

import com.springrag.user.domain.User;
import com.springrag.user.domain.UserRole;
import com.springrag.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Secure admin onboarding. There is intentionally NO API or profile path to become an admin
 * (privilege-escalation guard). Instead, an administrator is provisioned from server configuration:
 * set {@code ADMIN_EMAIL} and {@code ADMIN_PASSWORD} (env) and on startup this ensures that account
 * exists with {@code ROLE_ADMIN}. Then log in normally with those credentials — the issued JWT
 * carries {@code ROLE_ADMIN} and the Admin tab appears.
 *
 * <p>Idempotent: if the account already exists it is elevated to ADMIN; an existing password is not
 * clobbered on restart (only set on first creation). If no admin email is configured, this no-ops.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String username;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          @Value("${app.bootstrap-admin.email:}") String email,
                          @Value("${app.bootstrap-admin.password:}") String password,
                          @Value("${app.bootstrap-admin.username:Administrator}") String username) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.username = username;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank()) {
            log.info("No bootstrap admin configured (app.bootstrap-admin.email empty) — skipping.");
            return;
        }
        User user = userRepository.findByEmail(email).orElse(null);
        boolean isNew = user == null;
        if (isNew) {
            user = new User();
            user.setEmail(email);
            user.setUsername(username);
            user.setAuthProvider("LOCAL");
        }
        // Set a password only on first creation (or if the account never had one) — don't overwrite.
        if ((isNew || user.getPasswordHash() == null) && password != null && !password.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(password));
        }
        user.setRole(UserRole.ADMIN);
        userRepository.save(user);
        log.info("Bootstrap admin ensured: {} (role=ADMIN, {})", email, isNew ? "created" : "elevated");
    }
}
