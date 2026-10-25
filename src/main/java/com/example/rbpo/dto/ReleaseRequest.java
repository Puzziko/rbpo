package com.example.rbpo.dto;

import java.time.LocalDate;

public record ReleaseRequest(
        Long productId,
        String number,
        String status,
        LocalDate releaseDate
) {
}