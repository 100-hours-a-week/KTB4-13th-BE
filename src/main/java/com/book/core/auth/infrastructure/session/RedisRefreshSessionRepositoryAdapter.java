package com.book.core.auth.infrastructure.session;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.auth.application.port.RefreshSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
class RedisRefreshSessionRepositoryAdapter implements RefreshSessionRepository {
    private static final Logger log = LoggerFactory.getLogger(RedisRefreshSessionRepositoryAdapter.class);
    private final StringRedisTemplate redis;
    private final MeterRegistry meters;
    private final DefaultRedisScript<Long> login;
    private final DefaultRedisScript<Long> rotation;

    RedisRefreshSessionRepositoryAdapter(final StringRedisTemplate redis, final MeterRegistry meters) {
        this.redis = redis;
        this.meters = meters;
        this.login = script("auth/refresh-session/login.lua");
        this.rotation = script("auth/refresh-session/rotate.lua");
    }

    @Override
    public void replace(final Long userId, final String tokenHash, final Instant expiresAt) {
        final Long result =
            invoke("replace", () -> redis.execute(login, List.of(key(userId)), tokenHash, Long.toString(expiresAt.toEpochMilli())));
        if (!Long.valueOf(1).equals(result)) {
            throw new BusinessException(ErrorCode.TOKEN_ISSUE_FAILURE);
        }
        monitorRollback("replace");
    }

    @Override
    public boolean rotate(final Long userId, final String expectedHash, final String newHash, final Instant expiresAt) {
        final Long result = invoke("rotate",
            () -> redis.execute(rotation, List.of(key(userId)), expectedHash, newHash, Long.toString(expiresAt.toEpochMilli())));
        if (Long.valueOf(-1).equals(result) || result == null) {
            throw new BusinessException(ErrorCode.TOKEN_ISSUE_FAILURE);
        }
        if (Long.valueOf(1).equals(result)) {
            monitorRollback("rotate");
            return true;
        }
        return false;
    }

    @Override
    public void revoke(final Long userId) {
        invoke("revoke", () -> redis.delete(key(userId)));
        monitorRollback("revoke");
    }

    private void monitorRollback(final String operation) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(final int status) {
                    if (status != STATUS_COMMITTED) {
                        meters.counter("auth.refresh_session.transaction_not_committed", "operation", operation).increment();
                        log.warn("Redis refresh session changed but the database transaction did not commit: operation={}", operation);
                    }
                }
            });
        }
    }

    private String key(final Long userId) {
        return "auth:refresh-session:v1:{" + userId + "}";
    }

    private DefaultRedisScript<Long> script(final String path) {
        final DefaultRedisScript<Long> result = new DefaultRedisScript<>();
        result.setLocation(new ClassPathResource(path));
        result.setResultType(Long.class);
        return result;
    }

    private <T> T invoke(final String operation, final Supplier<T> action) {
        try {
            return action.get();
        } catch (final DataAccessException exception) {
            meters.counter("auth.refresh_session.redis_result_unconfirmed", "operation", operation).increment();
            log.warn("Redis refresh session operation outcome could not be confirmed: operation={}", operation);
            throw new BusinessException(ErrorCode.AUTH_SESSION_UNAVAILABLE, exception);
        }
    }
}
