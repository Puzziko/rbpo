package com.example.rbpo.dto;

public record SoftwareProductRequest(
        String name,
        Long responsibleId,
        String currentVersion
) {
}