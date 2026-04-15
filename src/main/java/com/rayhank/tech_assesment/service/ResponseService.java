package com.rayhank.tech_assesment.service;

import com.rayhank.tech_assesment.dto.MessageResponse;
import com.rayhank.tech_assesment.dto.response.*;
import com.rayhank.tech_assesment.entity.*;
import com.rayhank.tech_assesment.exception.*;
import com.rayhank.tech_assesment.repository.FormRepository;
import com.rayhank.tech_assesment.repository.QuestionRepository;
import com.rayhank.tech_assesment.repository.ResponseRepository;
import com.rayhank.tech_assesment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResponseService {

    private final FormRepository formRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final ResponseRepository responseRepository;

    @Transactional
    public MessageResponse submitResponse(String formSlug, SubmitResponseRequest request) {
        Form form = formRepository.findBySlug(formSlug)
                .orElseThrow(FormNotFoundException::new);

        User user = getCurrentUser();
        checkDomainAccess(form, user.getEmail());

        if (form.isLimitOneResponse() && responseRepository.existsByFormAndUser(form, user)) {
            throw new AlreadySubmittedException();
        }

        validateRequiredAnswers(form, request.getAnswers());

        Response response = new Response();
        response.setForm(form);
        response.setUser(user);
        response.setDate(LocalDateTime.now());

        List<Answer> answers = new ArrayList<>();
        for (AnswerRequest ar : request.getAnswers()) {
            Question question = questionRepository.findById(ar.getQuestionId())
                    .orElseThrow(QuestionNotFoundException::new);
            Answer answer = new Answer();
            answer.setResponse(response);
            answer.setQuestion(question);
            answer.setValue(ar.getValue());
            answers.add(answer);
        }
        response.setAnswers(answers);
        responseRepository.save(response);

        return new MessageResponse("Submit response success");
    }

    @Transactional(readOnly = true)
    public GetResponsesResponse getResponses(String formSlug) {
        Form form = formRepository.findBySlug(formSlug)
                .orElseThrow(FormNotFoundException::new);

        User currentUser = getCurrentUser();
        if (!form.getCreator().getEmail().equalsIgnoreCase(currentUser.getEmail())) {
            throw new ForbiddenAccessException();
        }

        List<ResponseDto> dtos = responseRepository.findByForm(form).stream()
                .map(this::toResponseDto)
                .toList();

        return GetResponsesResponse.builder()
                .message("Get responses success")
                .responses(dtos)
                .build();
    }

    // Validates that all required questions have a non-blank answer in the submission
    private void validateRequiredAnswers(Form form, List<AnswerRequest> answers) {
        List<Question> questions = form.getQuestions();
        if (questions == null || questions.isEmpty()) return;

        Set<Long> answeredIds = answers.stream()
                .filter(a -> a.getValue() != null && !a.getValue().isBlank())
                .map(AnswerRequest::getQuestionId)
                .collect(Collectors.toSet());

        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (Question q : questions) {
            if (q.isRequired() && !answeredIds.contains(q.getId())) {
                String key = "answers." + q.getId();
                errors.put(key, List.of("The answer for '" + q.getName() + "' is required."));
            }
        }

        if (!errors.isEmpty()) {
            throw new FormValidationException(errors);
        }
    }

    // Throws ForbiddenAccessException if the user's email domain is not in allowed_domains.
    // An empty allowed_domains list means the form is open to anyone.
    private void checkDomainAccess(Form form, String userEmail) {
        List<AllowedDomain> allowed = form.getAllowedDomains();
        if (allowed == null || allowed.isEmpty()) return;

        String userDomain = userEmail.substring(userEmail.lastIndexOf('@') + 1);
        boolean permitted = allowed.stream()
                .anyMatch(ad -> ad.getDomain().equalsIgnoreCase(userDomain));

        if (!permitted) throw new ForbiddenAccessException();
    }

    private ResponseDto toResponseDto(Response response) {
        UserResponseDto userDto = UserResponseDto.builder()
                .id(response.getUser().getId())
                .name(response.getUser().getName())
                .email(response.getUser().getEmail())
                .emailVerifiedAt(response.getUser().getEmailVerifiedAt())
                .build();

        // Build answers as question-name → answer-value map (preserving insertion order)
        Map<String, String> answersMap = new LinkedHashMap<>();
        if (response.getAnswers() != null) {
            for (Answer a : response.getAnswers()) {
                answersMap.put(a.getQuestion().getName(), a.getValue());
            }
        }

        return ResponseDto.builder()
                .date(response.getDate())
                .user(userDto)
                .answers(answersMap)
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));
    }
}
