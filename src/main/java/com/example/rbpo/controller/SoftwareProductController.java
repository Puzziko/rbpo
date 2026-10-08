package com.example.rbpo.controller;

import com.example.rbpo.entity.SoftwareProduct;
import com.example.rbpo.service.SoftwareProductService;
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
@RequestMapping("/api/software-products")
public class SoftwareProductController {

    private final SoftwareProductService softwareProductService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        List<Map<String, Object>> result = softwareProductService.getAll()
                .entrySet()
                .stream()
                .map(entry -> toResponse(entry.getKey(), entry.getValue()))
                .toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable String id) {
        return softwareProductService.getById(id)
                .map(product -> ResponseEntity.ok(toResponse(id, product)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody SoftwareProduct product
    ) {
        String id = softwareProductService.create(product);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(id, product));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable String id,
            @RequestBody SoftwareProduct patch
    ) {
        return softwareProductService.update(id, patch)
                .map(product -> ResponseEntity.ok(toResponse(id, product)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        softwareProductService.delete(id);
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