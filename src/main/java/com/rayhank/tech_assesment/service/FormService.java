package com.rayhank.tech_assesment.service;

import com.rayhank.tech_assesment.dto.form.CreateFormRequest;
import com.rayhank.tech_assesment.dto.form.CreateFormResponse;
import com.rayhank.tech_assesment.dto.form.FormDto;
import com.rayhank.tech_assesment.entity.AllowedDomain;
import com.rayhank.tech_assesment.entity.Form;
import com.rayhank.tech_assesment.entity.User;
import com.rayhank.tech_assesment.repository.FormRepository;
import com.rayhank.tech_assesment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FormService {

    private final FormRepository formRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreateFormResponse createForm(CreateFormRequest request) {
        User creator = getCurrentUser();

        Form form = new Form();
        form.setName(request.getName());
        form.setSlug(request.getSlug());
        form.setDescription(request.getDescription());
        form.setLimitOneResponse(request.isLimitOneResponse());
        form.setCreator(creator);

        if (request.getAllowedDomains() != null && !request.getAllowedDomains().isEmpty()) {
            List<AllowedDomain> domains = new ArrayList<>();
            for (String domain : request.getAllowedDomains()) {
                AllowedDomain allowedDomain = new AllowedDomain();
                allowedDomain.setForm(form);
                allowedDomain.setDomain(domain);
                domains.add(allowedDomain);
            }
            form.setAllowedDomains(domains);
        }

        Form saved = formRepository.save(form);

        return CreateFormResponse.builder()
                .message("Create form success")
                .form(FormDto.builder()
                        .id(saved.getId())
                        .name(saved.getName())
                        .slug(saved.getSlug())
                        .description(saved.getDescription())
                        .limitOneResponse(saved.isLimitOneResponse())
                        .creatorId(creator.getId())
                        .build())
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));
    }
}
