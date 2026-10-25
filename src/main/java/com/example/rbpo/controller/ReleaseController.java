package com.example.rbpo.controller;

import com.example.rbpo.dto.ReleaseRequest;
import com.example.rbpo.dto.ReleaseResponse;
import com.example.rbpo.service.ReleaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/releases")
public class ReleaseController {

    private final ReleaseService releaseService;

    @GetMapping
    public ResponseEntity<List<ReleaseResponse>> getAll() {
        return ResponseEntity.ok(releaseService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReleaseResponse> getById(
            @PathVariable Long id
    ) {
        return releaseService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ReleaseResponse> create(
            @RequestBody ReleaseRequest request
    ) {
        ReleaseResponse created = releaseService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ReleaseResponse> update(
            @PathVariable Long id,
            @RequestBody ReleaseRequest patch
    ) {
        return releaseService.update(id, patch)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!releaseService.delete(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ReleaseResponse> approve(
            @PathVariable Long id
    ) {
        return releaseService.approve(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ReleaseResponse> publish(
            @PathVariable Long id
    ) {
        return releaseService.publish(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}