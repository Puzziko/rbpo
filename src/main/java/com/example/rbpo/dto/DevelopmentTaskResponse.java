package com.example.rbpo.dto;

public record DevelopmentTaskResponse(
        Long id,
        Long productId,
        String productName,
        Long assigneeId,
        String assigneeUsername,
        String version,
        String readinessStatus
) {
}