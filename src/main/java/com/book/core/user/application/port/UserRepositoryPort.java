package com.book.core.user.application.port;

import com.book.core.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {
    User save(final User user);

    Optional<User> findById(final Long id);

    Optional<User> findByIdForUpdate(final Long id);

    List<Long> findWithdrawalTargetIds(final LocalDateTime requestedBefore);
}
