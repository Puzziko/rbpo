package com.example.rbpo.service;

import com.example.rbpo.entity.Release;
import com.example.rbpo.entity.SoftwareProduct;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReleaseService {

    private final Map<String, Release> releases = new LinkedHashMap<>();

    public Map<String, Release> getAll() {
        return releases;
    }

    public Optional<Release> getById(String id) {
        return Optional.ofNullable(releases.get(id));
    }

    public String create(Release release) {
        String id = UUID.randomUUID().toString();
        releases.put(id, release);
        return id;
    }

    public Optional<Release> update(String id, Release patch) {
        Release release = releases.get(id);

        if (release == null) {
            return Optional.empty();
        }

        if (patch.getProduct() != null) {
            release.setProduct(patch.getProduct());
        }

        if (patch.getNumber() != null) {
            release.setNumber(patch.getNumber());
        }

        if (patch.getStatus() != null) {
            release.setStatus(patch.getStatus());
        }

        if (patch.getReleaseDate() != null) {
            release.setReleaseDate(patch.getReleaseDate());
        }

        return Optional.of(release);
    }

    public void delete(String id) {
        releases.remove(id);
    }

    public void clearProduct(String productName) {
        releases.values().forEach(release -> {
            if (release.getProduct() != null &&
                    Objects.equals(release.getProduct().getName(), productName)) {
                release.setProduct(null);
            }
        });
    }

    public void replaceProduct(String oldProductName, SoftwareProduct newProduct) {
        releases.values().forEach(release -> {
            if (release.getProduct() != null &&
                    Objects.equals(release.getProduct().getName(), oldProductName)) {
                release.setProduct(newProduct);
            }
        });
    }
}