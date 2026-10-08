package com.example.rbpo.service;

import com.example.rbpo.entity.User;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserService {

    private final Map<String, User> users = new LinkedHashMap<>();

    private final SoftwareProductService softwareProductService;
    private final DevelopmentTaskService developmentTaskService;

    public UserService(
            SoftwareProductService softwareProductService,
            DevelopmentTaskService developmentTaskService
    ) {
        this.softwareProductService = softwareProductService;
        this.developmentTaskService = developmentTaskService;
    }

    public Map<String, User> getAll() {
        return users;
    }

    public Optional<User> getById(String id) {
        return Optional.ofNullable(users.get(id));
    }

    public String create(User user) {
        String id = UUID.randomUUID().toString();
        users.put(id, user);
        return id;
    }

    public Optional<User> update(String id, User patch) {
        User user = users.get(id);

        if (user == null) {
            return Optional.empty();
        }

        String oldUsername = user.getUsername();

        if (patch.getUsername() != null) {
            user.setUsername(patch.getUsername());
        }

        if (!Objects.equals(oldUsername, user.getUsername())) {
            softwareProductService.replaceResponsible(oldUsername, user);
            developmentTaskService.replaceAssignee(oldUsername, user);
        }

        return Optional.of(user);
    }

    public void delete(String id) {
        User user = users.remove(id);

        if (user != null) {
            softwareProductService.clearResponsible(user.getUsername());
            developmentTaskService.clearAssignee(user.getUsername());
        }
    }
}