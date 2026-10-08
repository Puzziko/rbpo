package com.example.rbpo.service;

import com.example.rbpo.entity.DevelopmentTask;
import com.example.rbpo.entity.SoftwareProduct;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class DevelopmentTaskService {

    private final Map<String, DevelopmentTask> tasks = new LinkedHashMap<>();

    public Map<String, DevelopmentTask> getAll() {
        return tasks;
    }

    public Optional<DevelopmentTask> getById(String id) {
        return Optional.ofNullable(tasks.get(id));
    }

    public String create(DevelopmentTask task) {
        String id = UUID.randomUUID().toString();
        tasks.put(id, task);
        return id;
    }

    public Optional<DevelopmentTask> update(String id, DevelopmentTask patch) {
        DevelopmentTask task = tasks.get(id);

        if (task == null) {
            return Optional.empty();
        }

        if (patch.getProduct() != null) {
            task.setProduct(patch.getProduct());
        }

        if (patch.getAssignee() != null) {
            task.setAssignee(patch.getAssignee());
        }

        if (patch.getVersion() != null) {
            task.setVersion(patch.getVersion());
        }

        if (patch.getReadinessStatus() != null) {
            task.setReadinessStatus(patch.getReadinessStatus());
        }

        return Optional.of(task);
    }

    public void delete(String id) {
        tasks.remove(id);
    }

    public void clearProduct(String productName) {
        tasks.values().forEach(task -> {
            if (task.getProduct() != null &&
                    Objects.equals(task.getProduct().getName(), productName)) {
                task.setProduct(null);
            }
        });
    }

    public void replaceProduct(String oldProductName, SoftwareProduct newProduct) {
        tasks.values().forEach(task -> {
            if (task.getProduct() != null &&
                    Objects.equals(task.getProduct().getName(), oldProductName)) {
                task.setProduct(newProduct);
            }
        });
    }

    public void clearAssignee(String username) {
        tasks.values().forEach(task -> {
            if (task.getAssignee() != null &&
                    Objects.equals(task.getAssignee().getUsername(), username)) {
                task.setAssignee(null);
            }
        });
    }

    public void replaceAssignee(String oldUsername, com.example.rbpo.entity.User newUser) {
        tasks.values().forEach(task -> {
            if (task.getAssignee() != null &&
                    Objects.equals(task.getAssignee().getUsername(), oldUsername)) {
                task.setAssignee(newUser);
            }
        });
    }
}