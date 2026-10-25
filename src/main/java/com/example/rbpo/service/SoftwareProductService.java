package com.example.rbpo.service;

import com.example.rbpo.dto.SoftwareProductRequest;
import com.example.rbpo.dto.SoftwareProductResponse;
import com.example.rbpo.entity.SoftwareProduct;
import com.example.rbpo.entity.User;
import com.example.rbpo.repository.DevelopmentTaskRepository;
import com.example.rbpo.repository.ReleaseRepository;
import com.example.rbpo.repository.SoftwareProductRepository;
import com.example.rbpo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SoftwareProductService {

    private final SoftwareProductRepository productRepository;
    private final UserRepository userRepository;
    private final DevelopmentTaskRepository taskRepository;
    private final ReleaseRepository releaseRepository;

    @Transactional(readOnly = true)
    public List<SoftwareProductResponse> getAll() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<SoftwareProductResponse> getById(Long id) {
        return productRepository.findById(id)
                .map(this::toResponse);
    }

    @Transactional
    public SoftwareProductResponse create(SoftwareProductRequest request) {
        validateText(request.name(), "Название продукта", 200);
        validateText(request.currentVersion(), "Текущая версия", 50);

        String name = request.name().trim();

        if (productRepository.existsByName(name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Продукт с таким названием уже существует"
            );
        }

        SoftwareProduct product = new SoftwareProduct();
        product.setName(name);
        product.setCurrentVersion(request.currentVersion().trim());
        product.setResponsible(findResponsible(request.responsibleId()));

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public Optional<SoftwareProductResponse> update(
            Long id,
            SoftwareProductRequest patch
    ) {
        Optional<SoftwareProduct> found = productRepository.findById(id);

        if (found.isEmpty()) {
            return Optional.empty();
        }

        SoftwareProduct product = found.get();

        if (patch.currentVersion() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Текущая версия изменяется только при публикации релиза"
            );
        }

        if (patch.name() != null) {
            validateText(patch.name(), "Название продукта", 200);

            String newName = patch.name().trim();

            if (!newName.equals(product.getName())
                    && productRepository.existsByName(newName)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Продукт с таким названием уже существует"
                );
            }

            product.setName(newName);
        }

        if (patch.currentVersion() != null) {
            validateText(patch.currentVersion(), "Текущая версия", 50);
            product.setCurrentVersion(patch.currentVersion().trim());
        }

        if (patch.responsibleId() != null) {
            product.setResponsible(findResponsible(patch.responsibleId()));
        }

        return Optional.of(toResponse(productRepository.save(product)));
    }

    @Transactional
    public boolean delete(Long id) {
        Optional<SoftwareProduct> found = productRepository.findById(id);

        if (found.isEmpty()) {
            return false;
        }

        if (taskRepository.existsByProduct_Id(id)
                || releaseRepository.existsByProduct_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Нельзя удалить продукт, пока с ним связаны задачи или релизы"
            );
        }

        productRepository.delete(found.get());
        return true;
    }

    private User findResponsible(Long responsibleId) {
        if (responsibleId == null) {
            return null;
        }

        return userRepository.findById(responsibleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ответственный пользователь не найден"
                ));
    }

    private SoftwareProductResponse toResponse(SoftwareProduct product) {
        User responsible = product.getResponsible();

        return new SoftwareProductResponse(
                product.getId(),
                product.getName(),
                responsible == null ? null : responsible.getId(),
                responsible == null ? null : responsible.getUsername(),
                product.getCurrentVersion()
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
}