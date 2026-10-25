package com.example.rbpo.dto;

import java.time.LocalDate;

public record ReleaseResponse(
        Long id,
        Long productId,
        String productName,
        String number,
        String status,
        LocalDate releaseDate
) {
}