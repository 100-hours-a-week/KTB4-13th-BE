package com.book.core.user.application.port;

import com.book.core.user.domain.User;
import java.util.Optional;

public interface UserRepository {
    User save(final User user);

    Optional<User> findById(final Long id);
}
