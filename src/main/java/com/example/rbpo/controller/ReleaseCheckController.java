package com.example.rbpo.controller;

import com.example.rbpo.entity.ReleaseCheck;
import com.example.rbpo.service.ReleaseCheckService;
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
@RequestMapping("/api/release-checks")
public class ReleaseCheckController {

    private final ReleaseCheckService releaseCheckService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        List<Map<String, Object>> result = releaseCheckService.getAll()
                .entrySet()
                .stream()
                .map(entry -> toResponse(entry.getKey(), entry.getValue()))
                .toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable String id) {
        return releaseCheckService.getById(id)
                .map(check -> ResponseEntity.ok(toResponse(id, check)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody ReleaseCheck check
    ) {
        String id = releaseCheckService.create(check);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(id, check));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable String id,
            @RequestBody ReleaseCheck patch
    ) {
        return releaseCheckService.update(id, patch)
                .map(check -> ResponseEntity.ok(toResponse(id, check)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        releaseCheckService.delete(id);
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