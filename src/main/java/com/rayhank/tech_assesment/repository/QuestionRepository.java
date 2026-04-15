package com.rayhank.tech_assesment.repository;

import com.rayhank.tech_assesment.entity.Form;
import com.rayhank.tech_assesment.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    Optional<Question> findByIdAndForm(Long id, Form form);
}
