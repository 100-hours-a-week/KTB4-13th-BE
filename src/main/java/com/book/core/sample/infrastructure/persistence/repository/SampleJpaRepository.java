package com.book.core.sample.infrastructure.persistence.repository;

import com.book.core.sample.domain.Sample;
import org.springframework.data.jpa.repository.JpaRepository;

interface SampleJpaRepository extends JpaRepository<Sample, Long> {}
