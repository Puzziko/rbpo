package com.example.rbpo.controller;

import com.example.rbpo.entity.User;
import com.example.rbpo.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        List<Map<String, Object>> result = userService.getAll()
                .entrySet()
                .stream()
                .map(entry -> toResponse(entry.getKey(), entry.getValue()))
                .toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable String id) {
        return userService.getById(id)
                .map(user -> ResponseEntity.ok(toResponse(id, user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody User user
    ) {
        String id = userService.create(user);
    
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(id, user));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable String id,
            @RequestBody User patch
    ) {
        return userService.update(id, patch)
                .map(user -> ResponseEntity.ok(toResponse(id, user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toResponse(String id, Object resource) {
        Map<String, Object> fields = objectMapper.convertValue(
                resource,
                new TypeReference<LinkedHashMap<String, Object>>() {}
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", id);
        response.putAll(fields);

        return response;
    }
}