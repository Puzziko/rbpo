package com.example.rbpo.service;

import org.springframework.stereotype.Service;
import com.example.rbpo.entity.SoftwareProduct;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class SoftwareProductService {

    private final Map<String, SoftwareProduct> products = new LinkedHashMap<>();

    private final DevelopmentTaskService developmentTaskService;
    private final ReleaseService releaseService;

    public SoftwareProductService(
            DevelopmentTaskService developmentTaskService,
            ReleaseService releaseService
    ) {
        this.developmentTaskService = developmentTaskService;
        this.releaseService = releaseService;
    }

    public Map<String, SoftwareProduct> getAll() {
        return products;
    }

    public Optional<SoftwareProduct> getById(String id) {
        return Optional.ofNullable(products.get(id));
    }

    public String create(SoftwareProduct product) {
        String id = UUID.randomUUID().toString();
        products.put(id, product);
        return id;
    }

    public Optional<SoftwareProduct> update(String id, SoftwareProduct patch) {
        SoftwareProduct product = products.get(id);

        if (product == null) {
            return Optional.empty();
        }

        String oldName = product.getName();

        if (patch.getName() != null) {
            product.setName(patch.getName());
        }

        if (patch.getResponsible() != null) {
            product.setResponsible(patch.getResponsible());
        }

        if (patch.getCurrentVersion() != null) {
            product.setCurrentVersion(patch.getCurrentVersion());
        }

        if (!Objects.equals(oldName, product.getName())) {
            developmentTaskService.replaceProduct(oldName, product);
            releaseService.replaceProduct(oldName, product);
        }

        return Optional.of(product);
    }

    public void delete(String id) {
        SoftwareProduct product = products.remove(id);

        if (product != null) {
            developmentTaskService.clearProduct(product.getName());
            releaseService.clearProduct(product.getName());
        }
    }

    public void clearResponsible(String username) {
        products.values().forEach(product -> {
            if (product.getResponsible() != null &&
                    Objects.equals(product.getResponsible().getUsername(), username)) {
                product.setResponsible(null);
            }
        });
    }

    public void replaceResponsible(String oldUsername, com.example.rbpo.entity.User newUser) {
        products.values().forEach(product -> {
            if (product.getResponsible() != null &&
                    Objects.equals(product.getResponsible().getUsername(), oldUsername)) {
                product.setResponsible(newUser);
            }
        });
    }
}