package com.rayhank.tech_assesment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "failed_jobs")
public class FailedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String uuid;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String connection;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String queue;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String exception;

    @Column(name = "failed_at", nullable = false)
    private Instant failedAt;
}
