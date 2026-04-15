package com.rayhank.tech_assesment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "forms")
public class Form {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    private String description;

    @Column(name = "limit_one_response", nullable = false)
    private boolean limitOneResponse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @OneToMany(mappedBy = "form", cascade = CascadeType.ALL)
    private List<Question> questions;

    @OneToMany(mappedBy = "form", cascade = CascadeType.ALL)
    private List<Response> responses;

    @OneToMany(mappedBy = "form", cascade = CascadeType.ALL)
    private List<AllowedDomain> allowedDomains;
}
