package com.example.rbpo.service;

import com.example.rbpo.dto.ReleaseCheckRequest;
import com.example.rbpo.dto.ReleaseCheckResponse;
import com.example.rbpo.entity.Release;
import com.example.rbpo.entity.ReleaseCheck;
import com.example.rbpo.entity.SoftwareProduct;
import com.example.rbpo.repository.ReleaseCheckRepository;
import com.example.rbpo.repository.ReleaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReleaseCheckService {

    private static final Set<String> ALLOWED_RESULTS =
            Set.of("PENDING", "PASSED", "FAILED", "SKIPPED");

    private final ReleaseCheckRepository checkRepository;
    private final ReleaseRepository releaseRepository;

    @Transactional(readOnly = true)
    public List<ReleaseCheckResponse> getAll() {
        return checkRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ReleaseCheckResponse> getById(Long id) {
        return checkRepository.findById(id)
                .map(this::toResponse);
    }

    @Transactional
    public ReleaseCheckResponse create(ReleaseCheckRequest request) {
        if (request.releaseId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Для проверки необходимо указать releaseId"
            );
        }

        String checkName = validateText(request.checkName());

        Release release = findRelease(request.releaseId());

        if (!"PLANNED".equals(release.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Добавлять проверки можно только к планируемому релизу"
            );
        }

        if (checkRepository.existsByRelease_IdAndCheckName(
                release.getId(), checkName)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Проверка с таким названием уже существует у этого релиза"
            );
        }

        boolean mandatory = request.mandatory() == null
                || request.mandatory();

        if (request.result() != null
                && !"PENDING".equalsIgnoreCase(request.result().trim())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Новая проверка должна начинаться со статуса PENDING"
            );
        }

        String result = "PENDING";

        ReleaseCheck check = new ReleaseCheck();
        check.setRelease(release);
        check.setCheckName(checkName);
        check.setMandatory(mandatory);
        check.setResult(result);

        return toResponse(checkRepository.save(check));
    }

    @Transactional
    public Optional<ReleaseCheckResponse> update(
            Long id,
            ReleaseCheckRequest patch
    ) {
        Optional<ReleaseCheck> found = checkRepository.findById(id);

        if (found.isEmpty()) {
            return Optional.empty();
        }

        if (patch.result() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Изменять результат нужно через операцию /result"
            );
        }

        ReleaseCheck check = found.get();

        if (!"PLANNED".equals(check.getRelease().getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Изменять параметры проверки можно только у планируемого релиза"
            );
        }

        Long targetReleaseId = patch.releaseId() == null
                ? check.getRelease().getId()
                : patch.releaseId();

        String targetCheckName = patch.checkName() == null
                ? check.getCheckName()
                : validateText(patch.checkName());

        boolean identityChanged =
                !targetReleaseId.equals(check.getRelease().getId())
                        || !targetCheckName.equals(check.getCheckName());

        if (identityChanged
                && checkRepository.existsByRelease_IdAndCheckName(
                targetReleaseId, targetCheckName)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Проверка с таким названием уже существует у этого релиза"
            );
        }

        boolean targetMandatory = patch.mandatory() == null
                ? check.isMandatory()
                : patch.mandatory();

        String targetResult = check.getResult();

        validateMandatoryResult(targetMandatory, targetResult);

        if (patch.releaseId() != null) {
            Release targetRelease = findRelease(patch.releaseId());

            if (!"PLANNED".equals(targetRelease.getStatus())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Нельзя переносить проверку в уже согласованный или опубликованный релиз"
                );
            }

            check.setRelease(targetRelease);
        }

        if (patch.checkName() != null) {
            check.setCheckName(targetCheckName);
        }

        check.setMandatory(targetMandatory);
        check.setResult(targetResult);

        return Optional.of(
                toResponse(checkRepository.save(check))
        );
    }

    @Transactional
    public Optional<ReleaseCheckResponse> recordResult(
            Long id,
            String result
    ) {
        Optional<ReleaseCheck> found = checkRepository.findById(id);

        if (found.isEmpty()) {
            return Optional.empty();
        }

        ReleaseCheck check = found.get();

        if (!"PLANNED".equals(check.getRelease().getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Нельзя менять результат проверки после согласования релиза"
            );
        }

        String normalized = normalizeResult(result, false);

        if ("PENDING".equals(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Результат должен быть PASSED, FAILED или SKIPPED"
            );
        }

        validateMandatoryResult(check.isMandatory(), normalized);

        check.setResult(normalized);

        return Optional.of(
                toResponse(checkRepository.save(check))
        );
    }

    @Transactional
    public boolean delete(Long id) {
        Optional<ReleaseCheck> found = checkRepository.findById(id);

        if (found.isEmpty()) {
            return false;
        }

        ReleaseCheck check = found.get();

        if (!"PLANNED".equals(check.getRelease().getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Нельзя удалять проверки согласованного или опубликованного релиза"
            );
        }

        checkRepository.delete(check);
        return true;
    }

    private Release findRelease(Long releaseId) {
        return releaseRepository.findById(releaseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Релиз не найден"
                ));
    }

    private ReleaseCheckResponse toResponse(ReleaseCheck check) {
        Release release = check.getRelease();
        SoftwareProduct product = release.getProduct();

        return new ReleaseCheckResponse(
                check.getId(),
                release.getId(),
                release.getNumber(),
                product.getId(),
                product.getName(),
                check.getCheckName(),
                check.isMandatory(),
                check.getResult()
        );
    }

    private String normalizeResult(
            String result,
            boolean allowDefault
    ) {
        if (result == null && allowDefault) {
            return "PENDING";
        }

        if (result == null || result.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Результат проверки не должен быть пустым"
            );
        }

        String normalized = result.trim().toUpperCase(Locale.ROOT);

        if (!ALLOWED_RESULTS.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Недопустимый результат проверки. Допустимы: "
                            + ALLOWED_RESULTS
            );
        }

        return normalized;
    }

    private String validateText(String checkName) {
        if (checkName == null || checkName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Название проверки не должно быть пустым"
            );
        }

        String normalized = checkName.trim();

        if (normalized.length() > 200) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Название проверки не должно превышать 200 символов"
            );
        }

        return normalized;
    }

    private void validateMandatoryResult(
            boolean mandatory,
            String result
    ) {
        if (mandatory && "SKIPPED".equals(result)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Обязательную проверку нельзя пропустить"
            );
        }
    }
}