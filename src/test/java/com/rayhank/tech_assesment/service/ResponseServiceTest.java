package com.rayhank.tech_assesment.service;

import com.rayhank.tech_assesment.dto.MessageResponse;
import com.rayhank.tech_assesment.dto.response.*;
import com.rayhank.tech_assesment.entity.*;
import com.rayhank.tech_assesment.exception.*;
import com.rayhank.tech_assesment.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResponseServiceTest {

    @Mock private FormRepository formRepository;
    @Mock private UserRepository userRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private ResponseRepository responseRepository;

    @InjectMocks private ResponseService responseService;

    private User creator;
    private User respondent;
    private Form form;

    @BeforeEach
    void setUp() {
        var auth = new UsernamePasswordAuthenticationToken("respondent@webtech.id", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        creator = new User();
        creator.setId(1L);
        creator.setName("Creator");
        creator.setEmail("creator@webtech.id");
        creator.setPassword("hashed");

        respondent = new User();
        respondent.setId(2L);
        respondent.setName("Respondent");
        respondent.setEmail("respondent@webtech.id");
        respondent.setPassword("hashed");

        form = new Form();
        form.setId(10L);
        form.setName("Test Form");
        form.setSlug("test-form");
        form.setLimitOneResponse(false);
        form.setCreator(creator);
        form.setAllowedDomains(List.of());
        form.setQuestions(List.of());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Question question(Long id, Form f, String name, boolean required) {
        Question q = new Question();
        q.setId(id);
        q.setForm(f);
        q.setName(name);
        q.setChoiceType(ChoiceType.SHORT_ANSWER);
        ReflectionTestUtils.setField(q, "isRequired", required);
        return q;
    }

    private AnswerRequest answerRequest(Long questionId, String value) {
        AnswerRequest ar = new AnswerRequest();
        ReflectionTestUtils.setField(ar, "questionId", questionId);
        ReflectionTestUtils.setField(ar, "value", value);
        return ar;
    }

    private SubmitResponseRequest submitRequest(List<AnswerRequest> answers) {
        SubmitResponseRequest req = new SubmitResponseRequest();
        ReflectionTestUtils.setField(req, "answers", answers);
        return req;
    }

    private AllowedDomain allowedDomain(Form f, String domain) {
        AllowedDomain ad = new AllowedDomain();
        ad.setForm(f);
        ad.setDomain(domain);
        return ad;
    }

    // =========================================================================
    // submitResponse
    // =========================================================================

    @Test
    @DisplayName("submitResponse returns message 'Submit response success'")
    void submitResponse_shouldReturnSuccessMessage() {
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(form));
        when(userRepository.findByEmail("respondent@webtech.id")).thenReturn(Optional.of(respondent));
        when(responseRepository.save(any(Response.class))).thenAnswer(i -> i.getArgument(0));

        MessageResponse response = responseService.submitResponse("test-form",
                submitRequest(List.of()));

        assertThat(response.getMessage()).isEqualTo("Submit response success");
    }

    @Test
    @DisplayName("submitResponse saves a Response entity linked to the form and current user")
    void submitResponse_shouldSaveResponseWithFormAndUser() {
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(form));
        when(userRepository.findByEmail("respondent@webtech.id")).thenReturn(Optional.of(respondent));
        when(responseRepository.save(any(Response.class))).thenAnswer(i -> i.getArgument(0));

        responseService.submitResponse("test-form", submitRequest(List.of()));

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(responseRepository).save(captor.capture());
        assertThat(captor.getValue().getForm().getId()).isEqualTo(10L);
        assertThat(captor.getValue().getUser().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("submitResponse saves Answer entities for each item in the request")
    void submitResponse_shouldSaveAnswersForEachRequestItem() {
        Question q1 = question(1L, form, "Name", false);
        Question q2 = question(2L, form, "City", false);
        form.setQuestions(List.of(q1, q2));

        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(form));
        when(userRepository.findByEmail("respondent@webtech.id")).thenReturn(Optional.of(respondent));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(q1));
        when(questionRepository.findById(2L)).thenReturn(Optional.of(q2));
        when(responseRepository.save(any(Response.class))).thenAnswer(i -> i.getArgument(0));

        SubmitResponseRequest request = submitRequest(List.of(
                answerRequest(1L, "Alice"),
                answerRequest(2L, "Jakarta")
        ));
        responseService.submitResponse("test-form", request);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(responseRepository).save(captor.capture());
        assertThat(captor.getValue().getAnswers()).hasSize(2);
    }

    @Test
    @DisplayName("submitResponse with unknown slug throws FormNotFoundException")
    void submitResponse_withUnknownSlug_shouldThrowFormNotFoundException() {
        when(formRepository.findBySlug("no-form")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> responseService.submitResponse("no-form", submitRequest(List.of())))
                .isInstanceOf(FormNotFoundException.class);
    }

    @Test
    @DisplayName("submitResponse with forbidden domain throws ForbiddenAccessException")
    void submitResponse_withForbiddenDomain_shouldThrowForbiddenAccessException() {
        form.setAllowedDomains(List.of(allowedDomain(form, "other.org")));
        // respondent@webtech.id domain is "webtech.id", not "other.org"
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(form));
        when(userRepository.findByEmail("respondent@webtech.id")).thenReturn(Optional.of(respondent));

        assertThatThrownBy(() -> responseService.submitResponse("test-form", submitRequest(List.of())))
                .isInstanceOf(ForbiddenAccessException.class);
    }

    @Test
    @DisplayName("submitResponse with matching allowed domain succeeds")
    void submitResponse_withMatchingDomain_shouldSucceed() {
        form.setAllowedDomains(List.of(allowedDomain(form, "webtech.id")));
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(form));
        when(userRepository.findByEmail("respondent@webtech.id")).thenReturn(Optional.of(respondent));
        when(responseRepository.save(any(Response.class))).thenAnswer(i -> i.getArgument(0));

        assertThatCode(() -> responseService.submitResponse("test-form", submitRequest(List.of())))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("submitResponse when limit_one_response and already submitted throws AlreadySubmittedException")
    void submitResponse_whenAlreadySubmitted_shouldThrowAlreadySubmittedException() {
        form.setLimitOneResponse(true);
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(form));
        when(userRepository.findByEmail("respondent@webtech.id")).thenReturn(Optional.of(respondent));
        when(responseRepository.existsByFormAndUser(form, respondent)).thenReturn(true);

        assertThatThrownBy(() -> responseService.submitResponse("test-form", submitRequest(List.of())))
                .isInstanceOf(AlreadySubmittedException.class)
                .hasMessage("You can not submit form twice");
    }

    @Test
    @DisplayName("submitResponse when limit_one_response=false allows multiple submissions")
    void submitResponse_withoutLimit_allowsMultipleSubmissions() {
        form.setLimitOneResponse(false);
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(form));
        when(userRepository.findByEmail("respondent@webtech.id")).thenReturn(Optional.of(respondent));
        when(responseRepository.save(any(Response.class))).thenAnswer(i -> i.getArgument(0));

        assertThatCode(() -> responseService.submitResponse("test-form", submitRequest(List.of())))
                .doesNotThrowAnyException();
        verify(responseRepository, never()).existsByFormAndUser(any(), any());
    }

    @Test
    @DisplayName("submitResponse when required question has no answer throws FormValidationException")
    void submitResponse_withMissingRequiredAnswer_shouldThrowFormValidationException() {
        Question required = question(1L, form, "Name", true);
        form.setQuestions(List.of(required));

        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(form));
        when(userRepository.findByEmail("respondent@webtech.id")).thenReturn(Optional.of(respondent));

        // Submit with empty answers — required question 1 is not answered
        assertThatThrownBy(() -> responseService.submitResponse("test-form", submitRequest(List.of())))
                .isInstanceOf(FormValidationException.class);
    }

    @Test
    @DisplayName("submitResponse when required question has blank value throws FormValidationException")
    void submitResponse_withBlankRequiredAnswer_shouldThrowFormValidationException() {
        Question required = question(1L, form, "Name", true);
        form.setQuestions(List.of(required));

        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(form));
        when(userRepository.findByEmail("respondent@webtech.id")).thenReturn(Optional.of(respondent));

        // Answer provided but blank
        SubmitResponseRequest request = submitRequest(List.of(answerRequest(1L, "   ")));

        assertThatThrownBy(() -> responseService.submitResponse("test-form", request))
                .isInstanceOf(FormValidationException.class);
    }

    @Test
    @DisplayName("submitResponse when required question has valid answer succeeds")
    void submitResponse_withRequiredAnswerProvided_shouldSucceed() {
        Question required = question(1L, form, "Name", true);
        form.setQuestions(List.of(required));

        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(form));
        when(userRepository.findByEmail("respondent@webtech.id")).thenReturn(Optional.of(respondent));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(required));
        when(responseRepository.save(any(Response.class))).thenAnswer(i -> i.getArgument(0));

        SubmitResponseRequest request = submitRequest(List.of(answerRequest(1L, "Alice")));

        assertThatCode(() -> responseService.submitResponse("test-form", request))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("submitResponse non-required question can be omitted without error")
    void submitResponse_nonRequiredQuestionOmitted_shouldSucceed() {
        Question optional = question(1L, form, "Notes", false);
        form.setQuestions(List.of(optional));

        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(form));
        when(userRepository.findByEmail("respondent@webtech.id")).thenReturn(Optional.of(respondent));
        when(responseRepository.save(any(Response.class))).thenAnswer(i -> i.getArgument(0));

        // No answers submitted — fine because question is not required
        assertThatCode(() -> responseService.submitResponse("test-form", submitRequest(List.of())))
                .doesNotThrowAnyException();
    }

    // =========================================================================
    // getResponses
    // =========================================================================

    @BeforeEach
    void setUpCreatorAuth() {
        // Override to creator for getResponses tests when needed
    }

    private void authenticateAs(String email) {
        var auth = new UsernamePasswordAuthenticationToken(email, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Response buildResponse(User user, Form f, LocalDateTime date) {
        Response r = new Response();
        r.setId(1L);
        r.setForm(f);
        r.setUser(user);
        r.setDate(date);
        r.setAnswers(List.of());
        return r;
    }

    @Test
    @DisplayName("getResponses returns message 'Get responses success'")
    void getResponses_shouldReturnCorrectMessage() {
        authenticateAs("creator@webtech.id");
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(form));
        when(userRepository.findByEmail("creator@webtech.id")).thenReturn(Optional.of(creator));
        when(responseRepository.findByForm(form)).thenReturn(List.of());

        GetResponsesResponse result = responseService.getResponses("test-form");

        assertThat(result.getMessage()).isEqualTo("Get responses success");
    }

    @Test
    @DisplayName("getResponses returns all responses for the form")
    void getResponses_shouldReturnAllResponses() {
        authenticateAs("creator@webtech.id");
        Response r1 = buildResponse(respondent, form, LocalDateTime.now());
        Response r2 = buildResponse(respondent, form, LocalDateTime.now());

        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(form));
        when(userRepository.findByEmail("creator@webtech.id")).thenReturn(Optional.of(creator));
        when(responseRepository.findByForm(form)).thenReturn(List.of(r1, r2));

        assertThat(responseService.getResponses("test-form").getResponses()).hasSize(2);
    }

    @Test
    @DisplayName("getResponses maps user info and answer map correctly")
    void getResponses_shouldMapFieldsCorrectly() {
        authenticateAs("creator@webtech.id");

        Question q = question(1L, form, "Name", false);
        Answer a = new Answer();
        a.setQuestion(q);
        a.setValue("Alice");

        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        Response r = buildResponse(respondent, form, now);
        r.setAnswers(List.of(a));

        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(form));
        when(userRepository.findByEmail("creator@webtech.id")).thenReturn(Optional.of(creator));
        when(responseRepository.findByForm(form)).thenReturn(List.of(r));

        ResponseDto dto = responseService.getResponses("test-form").getResponses().get(0);

        assertThat(dto.getDate()).isEqualTo(now);
        assertThat(dto.getUser().getId()).isEqualTo(2L);
        assertThat(dto.getUser().getName()).isEqualTo("Respondent");
        assertThat(dto.getUser().getEmail()).isEqualTo("respondent@webtech.id");
        assertThat(dto.getAnswers()).containsEntry("Name", "Alice");
    }

    @Test
    @DisplayName("getResponses with no responses returns empty list")
    void getResponses_withNoResponses_shouldReturnEmptyList() {
        authenticateAs("creator@webtech.id");
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(form));
        when(userRepository.findByEmail("creator@webtech.id")).thenReturn(Optional.of(creator));
        when(responseRepository.findByForm(form)).thenReturn(List.of());

        assertThat(responseService.getResponses("test-form").getResponses()).isEmpty();
    }

    @Test
    @DisplayName("getResponses with unknown slug throws FormNotFoundException")
    void getResponses_withUnknownSlug_shouldThrowFormNotFoundException() {
        authenticateAs("creator@webtech.id");
        when(formRepository.findBySlug("no-form")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> responseService.getResponses("no-form"))
                .isInstanceOf(FormNotFoundException.class);
    }

    @Test
    @DisplayName("getResponses by non-creator throws ForbiddenAccessException")
    void getResponses_byNonCreator_shouldThrowForbiddenAccessException() {
        // respondent is not the creator
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(form));
        when(userRepository.findByEmail("respondent@webtech.id")).thenReturn(Optional.of(respondent));

        assertThatThrownBy(() -> responseService.getResponses("test-form"))
                .isInstanceOf(ForbiddenAccessException.class);
    }

    @Test
    @DisplayName("getResponses by non-creator does not query responses")
    void getResponses_byNonCreator_shouldNotQueryResponses() {
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(form));
        when(userRepository.findByEmail("respondent@webtech.id")).thenReturn(Optional.of(respondent));

        assertThatThrownBy(() -> responseService.getResponses("test-form"))
                .isInstanceOf(ForbiddenAccessException.class);
        verify(responseRepository, never()).findByForm(any());
    }
}
