package com.example.rbpo.entity;

import lombok.Data;

@Data
public class DevelopmentTask {

    private SoftwareProduct product;
    private User assignee;
    private String version;
    private String readinessStatus;

}