package com.example.rbpo.service;

import com.example.rbpo.dto.LoginRequest;
import com.example.rbpo.dto.TokenResponse;
import com.example.rbpo.entity.User;
import com.example.rbpo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TokenService tokenService;

    public TokenResponse login(LoginRequest request) {
        if (request == null
                || request.username() == null
                || request.username().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Username обязателен"
            );
        }

        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Пароль обязателен"
            );
        }

        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );
        } catch (AuthenticationException e) {
            // Неверный пароль, неизвестный, отключённый или
            // заблокированный пользователь — один и тот же ответ.
            throw invalidCredentials();
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(this::invalidCredentials);

        return tokenService.issueTokenPair(user, request.deviceId());
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Неверное имя пользователя или пароль"
        );
    }
}
