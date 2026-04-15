package com.rayhank.tech_assesment.repository;

import com.rayhank.tech_assesment.entity.Form;
import com.rayhank.tech_assesment.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FormRepository extends JpaRepository<Form, Long> {

    boolean existsBySlug(String slug);

    List<Form> findAllByCreator(User creator);

    Optional<Form> findBySlug(String slug);
}
