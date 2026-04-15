package com.rayhank.tech_assesment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "password_resets")
public class PasswordReset {

    @Id
    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String token;

    @Column(name = "created_at")
    private Instant createdAt;
}
