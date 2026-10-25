package com.example.rbpo.service;

import com.example.rbpo.dto.ReleaseRequest;
import com.example.rbpo.dto.ReleaseResponse;
import com.example.rbpo.entity.DevelopmentTask;
import com.example.rbpo.entity.Release;
import com.example.rbpo.entity.ReleaseCheck;
import com.example.rbpo.entity.SoftwareProduct;
import com.example.rbpo.repository.DevelopmentTaskRepository;
import com.example.rbpo.repository.ReleaseCheckRepository;
import com.example.rbpo.repository.ReleaseRepository;
import com.example.rbpo.repository.SoftwareProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReleaseService {

    private final ReleaseRepository releaseRepository;
    private final SoftwareProductRepository productRepository;
    private final DevelopmentTaskRepository taskRepository;
    private final ReleaseCheckRepository checkRepository;

    @Transactional(readOnly = true)
    public List<ReleaseResponse> getAll() {
        return releaseRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ReleaseResponse> getById(Long id) {
        return releaseRepository.findById(id)
                .map(this::toResponse);
    }

    // Создание релиза: только начальный статус PLANNED.
    @Transactional
    public ReleaseResponse create(ReleaseRequest request) {
        if (request.productId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Для релиза необходимо указать productId"
            );
        }

        String number = validateText(
                request.number(), "Номер релиза", 50
        );

        if (request.status() != null
                && !"PLANNED".equalsIgnoreCase(request.status().trim())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Новый релиз должен создаваться со статусом PLANNED"
            );
        }

        if (request.releaseDate() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Дата публикации устанавливается только при публикации релиза"
            );
        }

        SoftwareProduct product = findProduct(request.productId());

        if (releaseRepository.existsByProduct_IdAndNumber(
                product.getId(), number)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Релиз с таким номером уже существует у этого продукта"
            );
        }

        Release release = new Release();
        release.setProduct(product);
        release.setNumber(number);
        release.setStatus("PLANNED");
        release.setReleaseDate(null);

        return toResponse(releaseRepository.save(release));
    }

    // Редактирование реквизитов возможно только для PLANNED.
    // Статус и дату публикации меняют бизнес-операции.
    @Transactional
    public Optional<ReleaseResponse> update(
            Long id,
            ReleaseRequest patch
    ) {
        Optional<Release> found = releaseRepository.findById(id);

        if (found.isEmpty()) {
            return Optional.empty();
        }

        Release release = found.get();

        if (!"PLANNED".equals(release.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Изменять реквизиты можно только у планируемого релиза"
            );
        }

        if (patch.status() != null || patch.releaseDate() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Статус и дату меняют специальные бизнес-операции"
            );
        }

        Long targetProductId = patch.productId() == null
                ? release.getProduct().getId()
                : patch.productId();

        String targetNumber = patch.number() == null
                ? release.getNumber()
                : validateText(patch.number(), "Номер релиза", 50);

        boolean identityChanged =
                !targetProductId.equals(release.getProduct().getId())
                        || !targetNumber.equals(release.getNumber());

        if (identityChanged
                && releaseRepository.existsByProduct_IdAndNumber(
                targetProductId, targetNumber)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Релиз с таким номером уже существует у этого продукта"
            );
        }

        if (patch.productId() != null) {
            release.setProduct(findProduct(patch.productId()));
        }

        if (patch.number() != null) {
            release.setNumber(targetNumber);
        }

        return Optional.of(
                toResponse(releaseRepository.save(release))
        );
    }

    @Transactional
    public boolean delete(Long id) {
        Optional<Release> found = releaseRepository.findById(id);

        if (found.isEmpty()) {
            return false;
        }

        Release release = found.get();

        if (!"PLANNED".equals(release.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Нельзя удалять согласованный или опубликованный релиз"
            );
        }

        releaseRepository.delete(release);
        return true;
    }

    // Бизнес-операция 4: согласовать релиз.
    @Transactional
    public Optional<ReleaseResponse> approve(Long id) {
        Optional<Release> found = releaseRepository.findById(id);

        if (found.isEmpty()) {
            return Optional.empty();
        }

        Release release = found.get();

        if (!"PLANNED".equals(release.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Согласовать можно только релиз в статусе PLANNED"
            );
        }

        validateReleaseReady(release);

        release.setStatus("APPROVED");

        return Optional.of(
                toResponse(releaseRepository.save(release))
        );
    }

    // Бизнес-операция 5: опубликовать согласованный релиз.
    @Transactional
    public Optional<ReleaseResponse> publish(Long id) {
        Optional<Release> found = releaseRepository.findById(id);

        if (found.isEmpty()) {
            return Optional.empty();
        }

        Release release = found.get();

        if (!"APPROVED".equals(release.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Публиковать можно только согласованный релиз"
            );
        }

        validateReleaseReady(release);

        SoftwareProduct product = release.getProduct();

        product.setCurrentVersion(release.getNumber());
        productRepository.save(product);

        release.setStatus("RELEASED");
        release.setReleaseDate(java.time.LocalDate.now());

        return Optional.of(
                toResponse(releaseRepository.save(release))
        );
    }

    private SoftwareProduct findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Программный продукт не найден"
                ));
    }

    private ReleaseResponse toResponse(Release release) {
        SoftwareProduct product = release.getProduct();

        return new ReleaseResponse(
                release.getId(),
                product.getId(),
                product.getName(),
                release.getNumber(),
                release.getStatus(),
                release.getReleaseDate()
        );
    }

    private String validateText(
            String value,
            String field,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    field + " не должен быть пустым"
            );
        }

        String normalized = value.trim();

        if (normalized.length() > maxLength) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    field + ": превышена максимальная длина " + maxLength
            );
        }

        return normalized;
    }

    private void validateReleaseReady(Release release) {
        Long productId = release.getProduct().getId();
        String version = release.getNumber();

        List<DevelopmentTask> tasks =
                taskRepository.findByProduct_IdAndVersion(
                        productId, version
                );

        if (tasks.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Нет задач для продукта и версии этого релиза"
            );
        }

        boolean allTasksReady = tasks.stream().allMatch(task ->
                ("READY".equals(task.getReadinessStatus())
                        || "DONE".equals(task.getReadinessStatus()))
                        && task.getAssignee() != null
        );

        if (!allTasksReady) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Не все задачи релиза готовы и имеют исполнителя"
            );
        }

        List<ReleaseCheck> checks =
                checkRepository.findByRelease_Id(release.getId());

        List<ReleaseCheck> mandatoryChecks = checks.stream()
                .filter(ReleaseCheck::isMandatory)
                .toList();

        if (mandatoryChecks.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Для согласования требуется хотя бы одна обязательная проверка"
            );
        }

        boolean allMandatoryPassed = mandatoryChecks.stream()
                .allMatch(check -> "PASSED".equals(check.getResult()));

        if (!allMandatoryPassed) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Не все обязательные проверки релиза пройдены"
            );
        }
    }
}