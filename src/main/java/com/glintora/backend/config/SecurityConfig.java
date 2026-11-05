package com.glintora.backend.config;

import com.glintora.backend.model.Customer;
import com.glintora.backend.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Form-login security. The storefront is public; checkout and order history
 * require a customer session; /admin/** requires ROLE_ADMIN. Accounts live
 * in the customers table (role column carries ROLE_CUSTOMER / ROLE_ADMIN).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomerRepository customerRepo;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/checkout/**", "/orders/**", "/account/**").authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", false)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?logout")
                        .permitAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            Customer customer = customerRepo.findByEmailIgnoreCase(username)
                    .orElseThrow(() -> new UsernameNotFoundException("No account for " + username));
            return User.withUsername(customer.getEmail())
                    .password(customer.getPasswordHash())
                    .authorities(new SimpleGrantedAuthority(customer.getRole()))
                    .build();
        };
    }
}
