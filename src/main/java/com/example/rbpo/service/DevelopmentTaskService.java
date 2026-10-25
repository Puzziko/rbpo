package com.example.rbpo.service;

import com.example.rbpo.dto.DevelopmentTaskRequest;
import com.example.rbpo.dto.DevelopmentTaskResponse;
import com.example.rbpo.entity.DevelopmentTask;
import com.example.rbpo.entity.SoftwareProduct;
import com.example.rbpo.entity.User;
import com.example.rbpo.repository.DevelopmentTaskRepository;
import com.example.rbpo.repository.SoftwareProductRepository;
import com.example.rbpo.repository.UserRepository;
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
public class DevelopmentTaskService {

    private static final Set<String> ALLOWED_STATUSES =
            Set.of("TODO", "IN_PROGRESS", "READY", "DONE");

    private final DevelopmentTaskRepository taskRepository;
    private final SoftwareProductRepository productRepository;
    private final UserRepository userRepository;
    private final ReleaseRepository releaseRepository;

    @Transactional(readOnly = true)
    public List<DevelopmentTaskResponse> getAll() {
        return taskRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<DevelopmentTaskResponse> getById(Long id) {
        return taskRepository.findById(id)
                .map(this::toResponse);
    }

    // CRUD: создаём только новую задачу в статусе TODO без исполнителя.
    @Transactional
    public DevelopmentTaskResponse create(DevelopmentTaskRequest request) {
        if (request.productId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Для задачи необходимо указать productId"
            );
        }

        validateText(request.version(), "Версия", 50);

        if (request.assigneeId() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Назначайте разработчика через операцию /assign"
            );
        }

        String status = request.readinessStatus() == null
                ? "TODO"
                : normalizeStatus(request.readinessStatus());

        if (!"TODO".equals(status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Новая задача должна иметь статус TODO"
            );
        }

        SoftwareProduct product = findProduct(request.productId());
        String version = request.version().trim();

        ensureVersionNotFrozen(product.getId(), version);

        DevelopmentTask task = new DevelopmentTask();
        task.setProduct(product);
        task.setAssignee(null);
        task.setVersion(version);
        task.setReadinessStatus("TODO");

        return toResponse(taskRepository.save(task));
    }

    // CRUD: редактируем сведения о задаче, но не её состояние.
    @Transactional
    public Optional<DevelopmentTaskResponse> update(
            Long id,
            DevelopmentTaskRequest patch
    ) {
        Optional<DevelopmentTask> found = taskRepository.findById(id);

        if (found.isEmpty()) {
            return Optional.empty();
        }

        if (patch.assigneeId() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Назначение разработчика выполняется через /assign"
            );
        }

        if (patch.readinessStatus() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Изменять статус задачи нужно через бизнес-операции"
            );
        }

        DevelopmentTask task = found.get();

        ensureTaskVersionNotFrozen(task);

        SoftwareProduct targetProduct = patch.productId() == null
                ? task.getProduct()
                : findProduct(patch.productId());

        String targetVersion = task.getVersion();

        if (patch.version() != null) {
            validateText(patch.version(), "Версия", 50);
            targetVersion = patch.version().trim();
        }

        ensureVersionNotFrozen(targetProduct.getId(), targetVersion);

        if (patch.productId() != null) {
            task.setProduct(targetProduct);
        }

        if (patch.version() != null) {
            task.setVersion(targetVersion);
        }

        return Optional.of(toResponse(taskRepository.save(task)));
    }

    @Transactional
    public boolean delete(Long id) {
        Optional<DevelopmentTask> found = taskRepository.findById(id);

        if (found.isEmpty()) {
            return false;
        }

        DevelopmentTask task = found.get();
        ensureTaskVersionNotFrozen(task);
        taskRepository.delete(task);
        return true;
    }

    // Бизнес-операция 1: назначение разработчика.
    @Transactional
    public Optional<DevelopmentTaskResponse> assignTask(
            Long taskId,
            Long assigneeId
    ) {
        if (assigneeId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Необходимо указать assigneeId"
            );
        }

        Optional<DevelopmentTask> found =
                taskRepository.findById(taskId);

        if (found.isEmpty()) {
            return Optional.empty();
        }

        DevelopmentTask task = found.get();
        ensureTaskVersionNotFrozen(task);
        String status = task.getReadinessStatus();

        if (!"TODO".equals(status) && !"IN_PROGRESS".equals(status)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Нельзя назначить или переназначить исполнителя "
                            + "для готовой или завершённой задачи"
            );
        }

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Пользователь для назначения не найден"
                ));

        task.setAssignee(assignee);

        // Назначение исполнителя переводит новую задачу в работу.
        if ("TODO".equals(status)) {
            task.setReadinessStatus("IN_PROGRESS");
        }

        return Optional.of(
                toResponse(taskRepository.save(task))
        );
    }

    // Бизнес-операция 2: подтвердить готовность задачи.
    @Transactional
    public Optional<DevelopmentTaskResponse> markTaskReady(Long taskId) {
        Optional<DevelopmentTask> found =
                taskRepository.findById(taskId);

        if (found.isEmpty()) {
            return Optional.empty();
        }

        DevelopmentTask task = found.get();

        ensureTaskVersionNotFrozen(task);

        if (!"IN_PROGRESS".equals(task.getReadinessStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Готовность можно подтвердить только для задачи "
                            + "со статусом IN_PROGRESS"
            );
        }

        if (task.getAssignee() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Нельзя подтвердить готовность задачи без исполнителя"
            );
        }

        task.setReadinessStatus("READY");

        return Optional.of(
                toResponse(taskRepository.save(task))
        );
    }

    private SoftwareProduct findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Программный продукт не найден"
                ));
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Статус задачи не должен быть пустым"
            );
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);

        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Недопустимый статус задачи. Допустимы: "
                            + ALLOWED_STATUSES
            );
        }

        return normalized;
    }

    private DevelopmentTaskResponse toResponse(DevelopmentTask task) {
        SoftwareProduct product = task.getProduct();
        User assignee = task.getAssignee();

        return new DevelopmentTaskResponse(
                task.getId(),
                product.getId(),
                product.getName(),
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getUsername(),
                task.getVersion(),
                task.getReadinessStatus()
        );
    }

    private void validateText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    field + " не должно быть пустым"
            );
        }

        if (value.trim().length() > maxLength) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    field + ": превышена максимальная длина " + maxLength
            );
        }
    }

    private void ensureVersionNotFrozen(Long productId, String version) {
        boolean frozen = releaseRepository
                .existsByProduct_IdAndNumberAndStatusIn(
                        productId,
                        version,
                        List.of("APPROVED", "RELEASED")
                );

        if (frozen) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Нельзя изменять задачи версии, которая уже согласована или опубликована"
            );
        }
    }

    private void ensureTaskVersionNotFrozen(DevelopmentTask task) {
        ensureVersionNotFrozen(
                task.getProduct().getId(),
                task.getVersion()
        );
    }
}