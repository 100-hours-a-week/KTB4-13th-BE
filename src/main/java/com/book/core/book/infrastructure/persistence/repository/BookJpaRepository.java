package com.book.core.book.infrastructure.persistence.repository;

import com.book.core.book.domain.Book;
import org.springframework.data.jpa.repository.JpaRepository;

interface BookJpaRepository extends JpaRepository<Book, Long> {}
