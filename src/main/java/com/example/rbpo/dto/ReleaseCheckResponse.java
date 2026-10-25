package com.example.rbpo.dto;

public record ReleaseCheckResponse(
        Long id,
        Long releaseId,
        String releaseNumber,
        Long productId,
        String productName,
        String checkName,
        boolean mandatory,
        String result
) {
}