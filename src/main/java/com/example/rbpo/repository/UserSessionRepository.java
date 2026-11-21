package com.example.rbpo.repository;

import com.example.rbpo.entity.UserSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository
        extends JpaRepository<UserSession, UUID> {

    // Блокировка строки (SELECT ... FOR UPDATE) не даёт двум параллельным
    // запросам с одним refresh одновременно выполнить обмен.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserSession> findByRefreshToken(String refreshToken);
}
