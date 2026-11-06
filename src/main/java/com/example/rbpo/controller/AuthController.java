package com.example.rbpo.controller;

import com.example.rbpo.dto.RegistrationRequest;
import com.example.rbpo.dto.UserResponse;
import com.example.rbpo.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegistrationService registrationService;

    @GetMapping("/csrf")
    public Map<String, String> getCsrfToken(CsrfToken csrfToken) {
        return Map.of(
                "headerName", csrfToken.getHeaderName(),
                "parameterName", csrfToken.getParameterName(),
                "token", csrfToken.getToken()
        );
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @RequestBody RegistrationRequest request
    ) {
        UserResponse response = registrationService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}