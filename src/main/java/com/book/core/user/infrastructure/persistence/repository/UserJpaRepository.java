package com.book.core.user.infrastructure.persistence.repository;

import com.book.core.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserJpaRepository extends JpaRepository<User, Long> {}
