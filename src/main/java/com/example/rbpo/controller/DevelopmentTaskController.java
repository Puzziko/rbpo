package com.example.rbpo.controller;

import com.example.rbpo.entity.DevelopmentTask;
import com.example.rbpo.service.DevelopmentTaskService;
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
@RequestMapping("/api/development-tasks")
public class DevelopmentTaskController {

    private final DevelopmentTaskService developmentTaskService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        List<Map<String, Object>> result = developmentTaskService.getAll()
                .entrySet()
                .stream()
                .map(entry -> toResponse(entry.getKey(), entry.getValue()))
                .toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable String id) {
        return developmentTaskService.getById(id)
                .map(task -> ResponseEntity.ok(toResponse(id, task)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody DevelopmentTask task
    ) {
        String id = developmentTaskService.create(task);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(id, task));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable String id,
            @RequestBody DevelopmentTask patch
    ) {
        return developmentTaskService.update(id, patch)
                .map(task -> ResponseEntity.ok(toResponse(id, task)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        developmentTaskService.delete(id);
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