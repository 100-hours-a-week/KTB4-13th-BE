package com.book.infrastructure.persistence.sample.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "samples")
public class SampleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    protected SampleEntity() {}

    public SampleEntity(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long id() { return id; }
    public String name() { return name; }
}
