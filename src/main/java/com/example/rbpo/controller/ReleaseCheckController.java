package com.example.rbpo.controller;

import com.example.rbpo.dto.ReleaseCheckRequest;
import com.example.rbpo.dto.ReleaseCheckResponse;
import com.example.rbpo.service.ReleaseCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.rbpo.dto.ReleaseCheckResultRequest;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/release-checks")
public class ReleaseCheckController {

    private final ReleaseCheckService releaseCheckService;

    @GetMapping
    public ResponseEntity<List<ReleaseCheckResponse>> getAll() {
        return ResponseEntity.ok(releaseCheckService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReleaseCheckResponse> getById(
            @PathVariable Long id
    ) {
        return releaseCheckService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ReleaseCheckResponse> create(
            @RequestBody ReleaseCheckRequest request
    ) {
        ReleaseCheckResponse created =
                releaseCheckService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ReleaseCheckResponse> update(
            @PathVariable Long id,
            @RequestBody ReleaseCheckRequest patch
    ) {
        return releaseCheckService.update(id, patch)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!releaseCheckService.delete(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/result")
    public ResponseEntity<ReleaseCheckResponse> recordResult(
            @PathVariable Long id,
            @RequestBody ReleaseCheckResultRequest request
    ) {
        return releaseCheckService.recordResult(id, request.result())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}