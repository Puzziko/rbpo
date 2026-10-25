package com.example.rbpo.dto;

public record DevelopmentTaskRequest(
        Long productId,
        Long assigneeId,
        String version,
        String readinessStatus
) {
}