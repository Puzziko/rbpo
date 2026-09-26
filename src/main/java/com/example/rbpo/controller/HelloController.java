package com.example.rbpo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class HelloController {

    @GetMapping("/api/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }

    @GetMapping("/api/numbers")
    public List<Integer> numbers() {
        return List.of(1, 2, 3, 4, 5);
    }
}