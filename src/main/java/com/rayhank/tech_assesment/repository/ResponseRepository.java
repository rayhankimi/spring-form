package com.rayhank.tech_assesment.repository;

import com.rayhank.tech_assesment.entity.Form;
import com.rayhank.tech_assesment.entity.Response;
import com.rayhank.tech_assesment.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResponseRepository extends JpaRepository<Response, Long> {

    boolean existsByFormAndUser(Form form, User user);

    List<Response> findByForm(Form form);
}
