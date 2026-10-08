package com.example.rbpo.service;

import com.example.rbpo.entity.ReleaseCheck;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReleaseCheckService {

    private final Map<String, ReleaseCheck> checks = new LinkedHashMap<>();

    public Map<String, ReleaseCheck> getAll() {
        return checks;
    }

    public Optional<ReleaseCheck> getById(String id) {
        return Optional.ofNullable(checks.get(id));
    }

    public String create(ReleaseCheck check) {
        String id = UUID.randomUUID().toString();
        checks.put(id, check);
        return id;
    }

    public Optional<ReleaseCheck> update(String id, ReleaseCheck patch) {
        ReleaseCheck check = checks.get(id);

        if (check == null) {
            return Optional.empty();
        }

        if (patch.getVersion() != null) {
            check.setVersion(patch.getVersion());
        }

        if (patch.getCheckName() != null) {
            check.setCheckName(patch.getCheckName());
        }

        check.setMandatory(patch.isMandatory());

        if (patch.getResult() != null) {
            check.setResult(patch.getResult());
        }

        return Optional.of(check);
    }

    public void delete(String id) {
        checks.remove(id);
    }
}