package com.book.infrastructure.persistence.sample.repository;

import com.book.infrastructure.persistence.sample.entity.SampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

interface SampleJpaRepository extends JpaRepository<SampleEntity, Long> {}
