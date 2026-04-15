package com.rayhank.tech_assesment.service;

import com.rayhank.tech_assesment.dto.form.*;
import com.rayhank.tech_assesment.entity.AllowedDomain;
import com.rayhank.tech_assesment.entity.Form;
import com.rayhank.tech_assesment.entity.Question;
import com.rayhank.tech_assesment.entity.User;
import com.rayhank.tech_assesment.exception.ForbiddenAccessException;
import com.rayhank.tech_assesment.exception.FormNotFoundException;
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
                .form(toFormDto(saved, creator.getId()))
                .build();
    }

    @Transactional(readOnly = true)
    public GetAllFormsResponse getAllForms() {
        User creator = getCurrentUser();

        List<FormDto> dtos = formRepository.findAllByCreator(creator)
                .stream()
                .map(form -> toFormDto(form, creator.getId()))
                .toList();

        return GetAllFormsResponse.builder()
                .message("Get all forms success")
                .forms(dtos)
                .build();
    }

    @Transactional(readOnly = true)
    public GetFormDetailResponse getFormBySlug(String slug) {
        Form form = formRepository.findBySlug(slug)
                .orElseThrow(FormNotFoundException::new);

        String userEmail = getAuthenticatedEmail();
        checkDomainAccess(form, userEmail);

        List<String> allowedDomains = form.getAllowedDomains() == null
                ? List.of()
                : form.getAllowedDomains().stream().map(AllowedDomain::getDomain).toList();

        List<QuestionDto> questions = form.getQuestions() == null
                ? List.of()
                : form.getQuestions().stream().map(this::toQuestionDto).toList();

        FormDetailDto detail = FormDetailDto.builder()
                .id(form.getId())
                .name(form.getName())
                .slug(form.getSlug())
                .description(form.getDescription())
                .limitOneResponse(form.isLimitOneResponse())
                .creatorId(form.getCreator().getId())
                .allowedDomains(allowedDomains)
                .questions(questions)
                .build();

        return GetFormDetailResponse.builder()
                .message("Get form success")
                .form(detail)
                .build();
    }

    // Throws ForbiddenAccessException if the user's email domain is not in allowed_domains.
    // If allowed_domains is empty, any authenticated user may access the form.
    private void checkDomainAccess(Form form, String userEmail) {
        List<AllowedDomain> allowed = form.getAllowedDomains();
        if (allowed == null || allowed.isEmpty()) return;

        String userDomain = userEmail.substring(userEmail.lastIndexOf('@') + 1);
        boolean permitted = allowed.stream()
                .anyMatch(ad -> ad.getDomain().equalsIgnoreCase(userDomain));

        if (!permitted) throw new ForbiddenAccessException();
    }

    private FormDto toFormDto(Form form, Long creatorId) {
        return FormDto.builder()
                .id(form.getId())
                .name(form.getName())
                .slug(form.getSlug())
                .description(form.getDescription())
                .limitOneResponse(form.isLimitOneResponse())
                .creatorId(creatorId)
                .build();
    }

    private QuestionDto toQuestionDto(Question question) {
        return QuestionDto.builder()
                .id(question.getId())
                .formId(question.getForm().getId())
                .name(question.getName())
                .choiceType(question.getChoiceType().toJsonValue())
                .choices(question.getChoices())
                .isRequired(question.isRequired())
                .build();
    }

    private User getCurrentUser() {
        return userRepository.findByEmail(getAuthenticatedEmail())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));
    }

    private String getAuthenticatedEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
