package com.rayhank.tech_assesment.repository;

import com.rayhank.tech_assesment.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}
