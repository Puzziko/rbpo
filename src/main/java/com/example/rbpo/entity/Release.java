package com.example.rbpo.entity;

import lombok.Data;

import java.time.LocalDate;

@Data
public class Release {

    private SoftwareProduct product;
    private String number;
    private String status;
    private LocalDate releaseDate;

}