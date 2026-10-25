package com.example.rbpo.controller;

import com.example.rbpo.dto.DevelopmentTaskRequest;
import com.example.rbpo.dto.DevelopmentTaskResponse;
import com.example.rbpo.service.DevelopmentTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.rbpo.dto.TaskAssignmentRequest;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/development-tasks")
public class DevelopmentTaskController {

    private final DevelopmentTaskService developmentTaskService;

    @GetMapping
    public ResponseEntity<List<DevelopmentTaskResponse>> getAll() {
        return ResponseEntity.ok(developmentTaskService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DevelopmentTaskResponse> getById(
            @PathVariable Long id
    ) {
        return developmentTaskService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<DevelopmentTaskResponse> create(
            @RequestBody DevelopmentTaskRequest request
    ) {
        DevelopmentTaskResponse created =
                developmentTaskService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DevelopmentTaskResponse> update(
            @PathVariable Long id,
            @RequestBody DevelopmentTaskRequest patch
    ) {
        return developmentTaskService.update(id, patch)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!developmentTaskService.delete(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<DevelopmentTaskResponse> assignTask(
            @PathVariable Long id,
            @RequestBody TaskAssignmentRequest request
    ) {
        return developmentTaskService.assignTask(id, request.assigneeId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/ready")
    public ResponseEntity<DevelopmentTaskResponse> markTaskReady(
            @PathVariable Long id
    ) {
        return developmentTaskService.markTaskReady(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}