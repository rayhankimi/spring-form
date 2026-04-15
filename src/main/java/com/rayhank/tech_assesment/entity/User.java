package com.rayhank.tech_assesment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(nullable = false)
    private String password;

    @Column(name = "remember_token", length = 100)
    private String rememberToken;

    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL)
    private List<Form> forms;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Response> responses;
}
