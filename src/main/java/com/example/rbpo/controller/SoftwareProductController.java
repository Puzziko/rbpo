package com.example.rbpo.controller;

import com.example.rbpo.dto.SoftwareProductRequest;
import com.example.rbpo.dto.SoftwareProductResponse;
import com.example.rbpo.service.SoftwareProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/software-products")
public class SoftwareProductController {

    private final SoftwareProductService softwareProductService;

    @GetMapping
    public ResponseEntity<List<SoftwareProductResponse>> getAll() {
        return ResponseEntity.ok(softwareProductService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SoftwareProductResponse> getById(
            @PathVariable Long id
    ) {
        return softwareProductService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<SoftwareProductResponse> create(
            @RequestBody SoftwareProductRequest request
    ) {
        SoftwareProductResponse created =
                softwareProductService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SoftwareProductResponse> update(
            @PathVariable Long id,
            @RequestBody SoftwareProductRequest patch
    ) {
        return softwareProductService.update(id, patch)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!softwareProductService.delete(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}