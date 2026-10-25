package com.example.rbpo.dto;

public record SoftwareProductResponse(
        Long id,
        String name,
        Long responsibleId,
        String responsibleUsername,
        String currentVersion
) {
}