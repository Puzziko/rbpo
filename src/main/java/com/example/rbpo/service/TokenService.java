package com.example.rbpo.service;

import com.example.rbpo.dto.TokenResponse;
import com.example.rbpo.entity.SessionStatus;
import com.example.rbpo.entity.User;
import com.example.rbpo.entity.UserSession;
import com.example.rbpo.repository.UserRepository;
import com.example.rbpo.repository.UserSessionRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;

    // Выдача пары после успешного входа или регистрации.
    // Если сессию сохранить не удалось, исключение откатывает транзакцию
    // (в том числе регистрацию, если метод вызван из неё).
    @Transactional
    public TokenResponse issueTokenPair(User user, String deviceId) {
        return createSession(
                user.getUsername(),
                normalizeDeviceId(deviceId)
        );
    }

    // Обновление пары. ResponseStatusException не откатывает транзакцию:
    // это нужно, чтобы перевод использованной сессии в REVOKED сохранился,
    // несмотря на отказ в HTTP-ответе. Технические ошибки (например,
    // DataAccessException) по-прежнему откатывают всю операцию.
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public TokenResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Refresh-токен обязателен"
            );
        }

        Claims claims;

        try {
            claims = jwtService.parseRefreshToken(refreshToken);
        } catch (JwtException e) {
            throw invalidRefresh();
        }

        UserSession session = sessionRepository
                .findByRefreshToken(refreshToken)
                .orElseThrow(this::invalidRefresh);

        Instant now = Instant.now();

        // Повторное предъявление уже использованного refresh:
        // отзываем запись и отказываем без новой пары.
        if (session.getStatus() == SessionStatus.USED) {
            if (session.getRefreshTokenExpiry().isAfter(now)) {
                session.setStatus(SessionStatus.REVOKED);
                sessionRepository.save(session);
            }

            throw invalidRefresh();
        }

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw invalidRefresh();
        }

        User user = userRepository.findByUsername(session.getUsername())
                .filter(found -> "ACTIVE".equals(found.getAccountStatus()))
                .orElseThrow(this::invalidRefresh);

        if (!session.getUsername().equals(claims.getSubject())) {
            throw invalidRefresh();
        }

        if (!session.getRefreshTokenExpiry().isAfter(now)) {
            throw invalidRefresh();
        }

        session.setStatus(SessionStatus.USED);
        sessionRepository.save(session);

        return createSession(user.getUsername(), session.getDeviceId());
    }

    private TokenResponse createSession(String username, String deviceId) {
        JwtService.IssuedToken access =
                jwtService.generateAccessToken(username);
        JwtService.IssuedToken refresh =
                jwtService.generateRefreshToken(username);

        UserSession session = new UserSession();
        session.setUsername(username);
        session.setDeviceId(deviceId);
        session.setAccessToken(access.token());
        session.setRefreshToken(refresh.token());
        session.setAccessTokenExpiry(access.expiresAt());
        session.setRefreshTokenExpiry(refresh.expiresAt());
        session.setStatus(SessionStatus.ACTIVE);

        // saveAndFlush: пара возвращается только после записи в БД.
        sessionRepository.saveAndFlush(session);

        return new TokenResponse(access.token(), refresh.token());
    }

    private String normalizeDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return null;
        }

        String normalized = deviceId.trim();

        if (normalized.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "deviceId не должен превышать 100 символов"
            );
        }

        return normalized;
    }

    // Одно и то же сообщение для всех причин отказа.
    private ResponseStatusException invalidRefresh() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Недействительный refresh-токен"
        );
    }
}
