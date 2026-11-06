package com.example.rbpo.service;

import com.example.rbpo.entity.User;
import com.example.rbpo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Неверное имя пользователя или пароль"
                ));

        if (user.getPasswordHash() == null) {
            throw new UsernameNotFoundException(
                    "Неверное имя пользователя или пароль"
            );
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities("ROLE_" + user.getRole())
                .disabled(!"ACTIVE".equals(user.getAccountStatus()))
                .accountLocked("LOCKED".equals(user.getAccountStatus()))
                .build();
    }
}