package com.glintora.backend.config;

import com.glintora.backend.model.Customer;
import com.glintora.backend.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the first admin account on an empty database so the admin dashboard
 * is reachable right after setup. Passwords are BCrypt-hashed at runtime.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CustomerRepository customerRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        boolean adminExists = customerRepo.findAll().stream()
                .anyMatch(c -> "ROLE_ADMIN".equals(c.getRole()));
        if (adminExists) {
            return;
        }
        Customer admin = Customer.builder()
                .name("Glintora Admin")
                .email("admin@glintora.com")
                .phone("+8801700000000")
                .passwordHash(passwordEncoder.encode("admin123"))
                .role("ROLE_ADMIN")
                .build();
        customerRepo.save(admin);
        log.info("Seeded admin account -> email: admin@glintora.com / password: admin123");
    }
}
