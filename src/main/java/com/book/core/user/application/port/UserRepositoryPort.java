package com.book.core.user.application.port;

import com.book.core.user.domain.User;
import java.util.Optional;

public interface UserRepositoryPort {
    User save(final User user);

    Optional<User> findById(final Long id);
}
