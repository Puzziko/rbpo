package com.example.rbpo.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final String ALGORITHM = "HS256";
    private static final String TYPE_CLAIM = "type";

    public static final String ACCESS_TYPE = "access";
    public static final String REFRESH_TYPE = "refresh";

    // Выданный токен и время его истечения (ровно как exp внутри JWT).
    public record IssuedToken(String token, Instant expiresAt) {
    }

    private final SecretKey signingKey;
    private final JwtParser parser;
    private final long accessTokenTtlSeconds;
    private final long refreshTokenTtlSeconds;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-ttl-seconds}") long accessTokenTtlSeconds,
            @Value("${jwt.refresh-token-ttl-seconds}") long refreshTokenTtlSeconds
    ) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        // Для HS256 ключ должен быть не короче 256 бит.
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT-ключ должен содержать не менее 32 байт"
            );
        }

        if (accessTokenTtlSeconds <= 0 || refreshTokenTtlSeconds <= 0) {
            throw new IllegalStateException(
                    "Время жизни токенов должно быть положительным"
            );
        }

        this.signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.parser = Jwts.parser().verifyWith(signingKey).build();
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    public IssuedToken generateAccessToken(String username) {
        return generate(username, ACCESS_TYPE, accessTokenTtlSeconds);
    }

    public IssuedToken generateRefreshToken(String username) {
        return generate(username, REFRESH_TYPE, refreshTokenTtlSeconds);
    }

    // Проверяет подпись, срок, обязательные claims и type = access.
    public Claims parseAccessToken(String token) {
        return parse(token, ACCESS_TYPE);
    }

    // Проверяет подпись, срок, обязательные claims и type = refresh.
    public Claims parseRefreshToken(String token) {
        return parse(token, REFRESH_TYPE);
    }

    private IssuedToken generate(
            String username,
            String type,
            long ttlSeconds
    ) {
        // JWT хранит время с точностью до секунды, поэтому округляем сразу,
        // чтобы значение в БД совпадало с exp внутри токена.
        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plusSeconds(ttlSeconds);

        // Новый jti для каждой выдачи.
        String token = Jwts.builder()
                .subject(username)
                .id(UUID.randomUUID().toString())
                .claim(TYPE_CLAIM, type)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();

        return new IssuedToken(token, expiresAt);
    }

    private Claims parse(String token, String expectedType) {
        try {
            Jws<Claims> jws = parser.parseSignedClaims(token);

            if (!ALGORITHM.equals(jws.getHeader().getAlgorithm())) {
                throw new JwtException("Недопустимый алгоритм подписи");
            }

            Claims claims = jws.getPayload();

            if (isBlank(claims.getSubject())
                    || isBlank(claims.getId())
                    || claims.getIssuedAt() == null
                    || claims.getExpiration() == null) {
                throw new JwtException("В токене нет обязательных claims");
            }

            String type = claims.get(TYPE_CLAIM, String.class);

            if (!expectedType.equals(type)) {
                throw new JwtException("Недопустимый тип токена");
            }

            return claims;
        } catch (IllegalArgumentException e) {
            // Например, пустая строка или null вместо токена.
            throw new JwtException("Некорректный токен", e);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
