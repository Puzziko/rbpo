package com.example.rbpo.service;

import com.example.rbpo.dto.UserRequest;
import com.example.rbpo.dto.UserResponse;
import com.example.rbpo.entity.User;
import com.example.rbpo.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<UserResponse> getById(Long id) {
        return userRepository.findById(id)
                .map(this::toResponse);
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        validateUsername(request.username());

        String username = request.username().trim();

        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Пользователь с таким username уже существует"
            );
        }

        User user = new User();
        user.setUsername(username);

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public Optional<UserResponse> update(
            Long id,
            UserRequest patch
    ) {
        Optional<User> found = userRepository.findById(id);

        if (found.isEmpty()) {
            return Optional.empty();
        }

        User user = found.get();

        if (patch.username() != null) {
            validateUsername(patch.username());

            String newUsername = patch.username().trim();

            if (!Objects.equals(user.getUsername(), newUsername)
                    && userRepository.existsByUsername(newUsername)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Пользователь с таким username уже существует"
                );
            }

            user.setUsername(newUsername);
        }

        return Optional.of(
                toResponse(userRepository.save(user))
        );
    }

    @Transactional
    public boolean delete(Long id) {
        Optional<User> found = userRepository.findById(id);

        if (found.isEmpty()) {
            return false;
        }

        userRepository.delete(found.get());
        return true;
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername()
        );
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Поле username не должно быть пустым"
            );
        }

        if (username.trim().length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Username не должен превышать 100 символов"
            );
        }
    }
}