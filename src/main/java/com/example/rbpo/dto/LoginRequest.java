package com.example.rbpo.dto;

public record LoginRequest(
        String username,
        String password,
        String deviceId
) {
}
