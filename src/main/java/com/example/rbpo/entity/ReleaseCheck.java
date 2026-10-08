package com.example.rbpo.entity;

import lombok.Data;

@Data
public class ReleaseCheck {

    private String version;
    private String checkName;
    private boolean mandatory;
    private String result;

}