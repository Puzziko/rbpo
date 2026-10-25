package com.example.rbpo.dto;

public record ReleaseCheckRequest(
        Long releaseId,
        String checkName,
        Boolean mandatory,
        String result
) {
}