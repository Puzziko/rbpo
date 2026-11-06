package com.example.rbpo.service;

import com.example.rbpo.entity.DevelopmentTask;
import com.example.rbpo.entity.Release;
import com.example.rbpo.entity.ReleaseCheck;
import com.example.rbpo.entity.User;
import com.example.rbpo.repository.DevelopmentTaskRepository;
import com.example.rbpo.repository.ReleaseCheckRepository;
import com.example.rbpo.repository.ReleaseRepository;
import com.example.rbpo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final UserRepository userRepository;
    private final DevelopmentTaskRepository taskRepository;
    private final ReleaseRepository releaseRepository;
    private final ReleaseCheckRepository checkRepository;

    // Получаем учётную запись текущего аутентифицированного пользователя.
    private Optional<User> getCurrentUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        return userRepository.findByUsername(authentication.getName())
                .filter(user -> "ACTIVE".equals(user.getAccountStatus()));
    }

    // USER может назначить себя только на свободную задачу TODO.
    @Transactional(readOnly = true)
    public boolean canSelfAssign(
            Authentication authentication,
            Long taskId,
            Long assigneeId
    ) {
        Optional<User> currentUser = getCurrentUser(authentication);

        if (currentUser.isEmpty() || assigneeId == null) {
            return false;
        }

        if (!currentUser.get().getId().equals(assigneeId)) {
            return false;
        }

        return taskRepository.findById(taskId)
                .filter(task -> task.getAssignee() == null)
                .filter(task -> "TODO".equals(task.getReadinessStatus()))
                .isPresent();
    }

    // USER может подтвердить готовность только своей задачи.
    @Transactional(readOnly = true)
    public boolean isAssignedToCurrentUser(
            Authentication authentication,
            Long taskId
    ) {
        Optional<User> currentUser = getCurrentUser(authentication);

        if (currentUser.isEmpty()) {
            return false;
        }

        return taskRepository.findById(taskId)
                .map(DevelopmentTask::getAssignee)
                .filter(assignee ->
                        assignee.getId().equals(currentUser.get().getId()))
                .isPresent();
    }

    // USER может управлять релизом только своего продукта.
    @Transactional(readOnly = true)
    public boolean isResponsibleForRelease(
            Authentication authentication,
            Long releaseId
    ) {
        Optional<User> currentUser = getCurrentUser(authentication);

        if (currentUser.isEmpty()) {
            return false;
        }

        return releaseRepository.findById(releaseId)
                .map(Release::getProduct)
                .map(product -> product.getResponsible())
                .filter(responsible ->
                        responsible.getId().equals(currentUser.get().getId()))
                .isPresent();
    }

    // USER может фиксировать проверки только своего продукта.
    @Transactional(readOnly = true)
    public boolean isResponsibleForCheck(
            Authentication authentication,
            Long checkId
    ) {
        Optional<User> currentUser = getCurrentUser(authentication);

        if (currentUser.isEmpty()) {
            return false;
        }

        return checkRepository.findById(checkId)
                .map(ReleaseCheck::getRelease)
                .map(Release::getProduct)
                .map(product -> product.getResponsible())
                .filter(responsible ->
                        responsible.getId().equals(currentUser.get().getId()))
                .isPresent();
    }
}