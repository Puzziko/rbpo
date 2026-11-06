package com.example.rbpo.service;

import com.example.rbpo.dto.RegistrationRequest;
import com.example.rbpo.dto.UserResponse;
import com.example.rbpo.entity.User;
import com.example.rbpo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(RegistrationRequest request) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Необходимо передать данные регистрации"
            );
        }

        String username = validateUsername(request.username());
        String password = request.password();

        validatePassword(password);

        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Пользователь с таким username уже существует"
            );
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));

        // Повышенные привилегии нельзя получить при регистрации.
        user.setRole("USER");
        user.setAccountStatus("ACTIVE");

        User saved = userRepository.save(user);

        // Не возвращаем пароль или его хеш клиенту.
        return new UserResponse(saved.getId(), saved.getUsername());
    }

    private String validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Username обязателен"
            );
        }

        String normalized = username.trim();

        if (!normalized.matches("[A-Za-z0-9][A-Za-z0-9._-]{2,99}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Username должен содержать от 3 до 100 символов: "
                            + "латинские буквы, цифры, точку, дефис или подчёркивание"
            );
        }

        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Пароль обязателен"
            );
        }

        if (password.length() < 12) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Пароль должен содержать не менее 12 символов"
            );
        }

        // BCrypt обычно ограничивает пароль 72 байтами.
        // Проверяем размер в UTF-8, а не только число символов.
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Пароль слишком длинный для выбранного алгоритма"
            );
        }

        boolean hasUppercase = password.chars()
                .anyMatch(Character::isUpperCase);
        boolean hasLowercase = password.chars()
                .anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars()
                .anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars()
                .anyMatch(ch -> !Character.isLetterOrDigit(ch));

        if (!hasUppercase || !hasLowercase || !hasDigit || !hasSpecial) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Пароль должен содержать заглавную и строчную буквы, "
                            + "цифру и специальный символ"
            );
        }
    }
}