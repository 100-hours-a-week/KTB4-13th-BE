package com.book.core.sample.infrastructure.persistence.repository;

import com.book.core.sample.infrastructure.persistence.entity.SampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

interface SampleJpaRepository extends JpaRepository<SampleEntity, Long> {}
