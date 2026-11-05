package com.glintora.backend.service;

import com.glintora.backend.model.Customer;
import com.glintora.backend.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Customer registration and account lookup. */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepo;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Customer register(String name, String email, String phone, String rawPassword) {
        if (name == null || name.isBlank()) {
            throw new InvalidOperationException("Name is required.");
        }
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new InvalidOperationException("A valid email address is required.");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new InvalidOperationException("Password must be at least 6 characters.");
        }
        if (customerRepo.existsByEmailIgnoreCase(email.trim())) {
            throw new InvalidOperationException("An account with that email already exists.");
        }
        Customer customer = Customer.builder()
                .name(name.trim())
                .email(email.trim().toLowerCase())
                .phone(phone)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role("ROLE_CUSTOMER")
                .build();
        return customerRepo.save(customer);
    }

    @Transactional(readOnly = true)
    public Customer byEmail(String email) {
        return customerRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidOperationException("Customer not found."));
    }
}
