package com.example.rbpo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/auth/csrf"
                ).permitAll()
                .requestMatchers(
                        HttpMethod.POST,
                        "/api/auth/register"
                ).permitAll()
                .anyRequest().authenticated()
        );

        http.httpBasic(Customizer.withDefaults());

        // CSRF-защита остаётся включённой.
        // Не вызываем csrf(csrf -> csrf.disable()).

        return http.build();
    }
}