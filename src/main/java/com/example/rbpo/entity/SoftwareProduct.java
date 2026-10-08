package com.example.rbpo.entity;

import lombok.Data;

@Data
public class SoftwareProduct {

    private String name;
    private User responsible;
    private String currentVersion;

}