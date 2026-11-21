package com.example.rbpo.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
