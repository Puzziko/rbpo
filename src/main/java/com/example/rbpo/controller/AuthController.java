package com.example.rbpo.controller;

import com.example.rbpo.dto.LoginRequest;
import com.example.rbpo.dto.RefreshRequest;
import com.example.rbpo.dto.RegistrationRequest;
import com.example.rbpo.dto.TokenResponse;
import com.example.rbpo.service.AuthService;
import com.example.rbpo.service.RegistrationService;
import com.example.rbpo.service.TokenService;
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
    private final AuthService authService;
    private final TokenService tokenService;

    @GetMapping("/csrf")
    public Map<String, String> getCsrfToken(CsrfToken csrfToken) {
        return Map.of(
                "headerName", csrfToken.getHeaderName(),
                "parameterName", csrfToken.getParameterName(),
                "token", csrfToken.getToken()
        );
    }

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(
            @RequestBody RegistrationRequest request
    ) {
        TokenResponse response = registrationService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @RequestBody RefreshRequest request
    ) {
        return ResponseEntity.ok(
                tokenService.refresh(request.refreshToken())
        );
    }
}
