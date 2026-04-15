package com.rayhank.tech_assesment.repository;

import com.rayhank.tech_assesment.entity.Form;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormRepository extends JpaRepository<Form, Long> {

    boolean existsBySlug(String slug);
}
