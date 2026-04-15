package com.rayhank.tech_assesment.service;

import com.rayhank.tech_assesment.dto.form.*;
import com.rayhank.tech_assesment.entity.*;
import com.rayhank.tech_assesment.dto.MessageResponse;
import com.rayhank.tech_assesment.exception.ForbiddenAccessException;
import com.rayhank.tech_assesment.exception.FormNotFoundException;
import com.rayhank.tech_assesment.exception.QuestionNotFoundException;
import com.rayhank.tech_assesment.repository.FormRepository;
import com.rayhank.tech_assesment.repository.QuestionRepository;
import com.rayhank.tech_assesment.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormServiceTest {

    @Mock private FormRepository formRepository;
    @Mock private UserRepository userRepository;
    @Mock private QuestionRepository questionRepository;

    @InjectMocks private FormService formService;

    private User creator;
    private Form savedForm;

    @BeforeEach
    void setUp() {
        var auth = new UsernamePasswordAuthenticationToken("user1@webtech.id", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        creator = new User();
        creator.setId(1L);
        creator.setName("User 1");
        creator.setEmail("user1@webtech.id");
        creator.setPassword("hashed");

        savedForm = new Form();
        savedForm.setId(10L);
        savedForm.setName("Test Form");
        savedForm.setSlug("test-form");
        savedForm.setDescription("A description");
        savedForm.setLimitOneResponse(false);
        savedForm.setCreator(creator);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private CreateFormRequest buildRequest(String name, String slug, String description,
                                           List<String> domains, boolean limitOne) {
        CreateFormRequest req = new CreateFormRequest();
        ReflectionTestUtils.setField(req, "name", name);
        ReflectionTestUtils.setField(req, "slug", slug);
        ReflectionTestUtils.setField(req, "description", description);
        ReflectionTestUtils.setField(req, "allowedDomains", domains);
        ReflectionTestUtils.setField(req, "limitOneResponse", limitOne);
        return req;
    }

    private AllowedDomain domain(Form form, String domainStr) {
        AllowedDomain ad = new AllowedDomain();
        ad.setForm(form);
        ad.setDomain(domainStr);
        return ad;
    }

    private Question question(Form form, String name, ChoiceType type, boolean required) {
        Question q = new Question();
        q.setForm(form);
        q.setName(name);
        q.setChoiceType(type);
        ReflectionTestUtils.setField(q, "isRequired", required);
        return q;
    }

    // =========================================================================
    // createForm
    // =========================================================================

    @Test
    @DisplayName("createForm returns CreateFormResponse with correct message and form data")
    void createForm_shouldReturnCreateFormResponse() {
        when(userRepository.findByEmail("user1@webtech.id")).thenReturn(Optional.of(creator));
        when(formRepository.save(any(Form.class))).thenReturn(savedForm);

        CreateFormRequest request = buildRequest("Test Form", "test-form", "A description", null, false);

        CreateFormResponse response = formService.createForm(request);

        assertThat(response.getMessage()).isEqualTo("Create form success");
        assertThat(response.getForm().getId()).isEqualTo(10L);
        assertThat(response.getForm().getName()).isEqualTo("Test Form");
        assertThat(response.getForm().getSlug()).isEqualTo("test-form");
        assertThat(response.getForm().getDescription()).isEqualTo("A description");
        assertThat(response.getForm().getCreatorId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("createForm sets creator from authenticated user in SecurityContext")
    void createForm_shouldSetCreatorFromSecurityContext() {
        when(userRepository.findByEmail("user1@webtech.id")).thenReturn(Optional.of(creator));
        when(formRepository.save(any(Form.class))).thenReturn(savedForm);

        formService.createForm(buildRequest("F", "f", null, null, false));

        ArgumentCaptor<Form> captor = ArgumentCaptor.forClass(Form.class);
        verify(formRepository).save(captor.capture());
        assertThat(captor.getValue().getCreator().getEmail()).isEqualTo("user1@webtech.id");
    }

    @Test
    @DisplayName("createForm with allowed_domains saves AllowedDomain entities on the form")
    void createForm_withAllowedDomains_shouldAttachDomainsToForm() {
        when(userRepository.findByEmail("user1@webtech.id")).thenReturn(Optional.of(creator));
        when(formRepository.save(any(Form.class))).thenReturn(savedForm);

        CreateFormRequest request = buildRequest("F", "f", null, List.of("webtech.id", "worldskills.org"), false);
        formService.createForm(request);

        ArgumentCaptor<Form> captor = ArgumentCaptor.forClass(Form.class);
        verify(formRepository).save(captor.capture());

        List<String> domains = captor.getValue().getAllowedDomains()
                .stream().map(AllowedDomain::getDomain).toList();

        assertThat(domains).containsExactlyInAnyOrder("webtech.id", "worldskills.org");
    }

    @Test
    @DisplayName("createForm with no allowed_domains saves form without domain entities")
    void createForm_withNullAllowedDomains_shouldSaveFormWithoutDomains() {
        when(userRepository.findByEmail("user1@webtech.id")).thenReturn(Optional.of(creator));
        when(formRepository.save(any(Form.class))).thenReturn(savedForm);

        formService.createForm(buildRequest("F", "f", null, null, false));

        ArgumentCaptor<Form> captor = ArgumentCaptor.forClass(Form.class);
        verify(formRepository).save(captor.capture());
        assertThat(captor.getValue().getAllowedDomains()).isNullOrEmpty();
    }

    @Test
    @DisplayName("createForm with limit_one_response=true sets field correctly on entity")
    void createForm_withLimitOneResponse_shouldSetTrueOnEntity() {
        savedForm.setLimitOneResponse(true);
        when(userRepository.findByEmail("user1@webtech.id")).thenReturn(Optional.of(creator));
        when(formRepository.save(any(Form.class))).thenReturn(savedForm);

        CreateFormRequest request = buildRequest("F", "f", null, null, true);
        CreateFormResponse response = formService.createForm(request);

        ArgumentCaptor<Form> captor = ArgumentCaptor.forClass(Form.class);
        verify(formRepository).save(captor.capture());
        assertThat(captor.getValue().isLimitOneResponse()).isTrue();
        assertThat(response.getForm().isLimitOneResponse()).isTrue();
    }

    @Test
    @DisplayName("createForm throws when authenticated user is not found in database")
    void createForm_whenUserNotInDb_shouldThrowIllegalStateException() {
        when(userRepository.findByEmail("user1@webtech.id")).thenReturn(Optional.empty());

        CreateFormRequest request = buildRequest("F", "f", null, null, false);

        assertThatThrownBy(() -> formService.createForm(request))
                .isInstanceOf(IllegalStateException.class);
    }

    // =========================================================================
    // getAllForms
    // =========================================================================

    @Test
    @DisplayName("getAllForms returns message 'Get all forms success'")
    void getAllForms_shouldReturnCorrectMessage() {
        when(userRepository.findByEmail("user1@webtech.id")).thenReturn(Optional.of(creator));
        when(formRepository.findAllByCreator(creator)).thenReturn(List.of());

        GetAllFormsResponse response = formService.getAllForms();

        assertThat(response.getMessage()).isEqualTo("Get all forms success");
    }

    @Test
    @DisplayName("getAllForms returns all forms belonging to the current user")
    void getAllForms_shouldReturnAllFormsOfCurrentUser() {
        Form form2 = new Form();
        form2.setId(11L);
        form2.setName("Form 2");
        form2.setSlug("form-2");
        form2.setCreator(creator);

        when(userRepository.findByEmail("user1@webtech.id")).thenReturn(Optional.of(creator));
        when(formRepository.findAllByCreator(creator)).thenReturn(List.of(savedForm, form2));

        GetAllFormsResponse response = formService.getAllForms();

        assertThat(response.getForms()).hasSize(2);
    }

    @Test
    @DisplayName("getAllForms returns empty list when user has no forms")
    void getAllForms_withNoForms_shouldReturnEmptyList() {
        when(userRepository.findByEmail("user1@webtech.id")).thenReturn(Optional.of(creator));
        when(formRepository.findAllByCreator(creator)).thenReturn(List.of());

        GetAllFormsResponse response = formService.getAllForms();

        assertThat(response.getForms()).isEmpty();
    }

    @Test
    @DisplayName("getAllForms maps all FormDto fields correctly")
    void getAllForms_shouldMapAllFieldsCorrectly() {
        savedForm.setLimitOneResponse(true);

        when(userRepository.findByEmail("user1@webtech.id")).thenReturn(Optional.of(creator));
        when(formRepository.findAllByCreator(creator)).thenReturn(List.of(savedForm));

        GetAllFormsResponse response = formService.getAllForms();

        FormDto dto = response.getForms().get(0);
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getName()).isEqualTo("Test Form");
        assertThat(dto.getSlug()).isEqualTo("test-form");
        assertThat(dto.getDescription()).isEqualTo("A description");
        assertThat(dto.isLimitOneResponse()).isTrue();
        assertThat(dto.getCreatorId()).isEqualTo(1L);
    }

    // =========================================================================
    // getFormBySlug
    // =========================================================================

    @Test
    @DisplayName("getFormBySlug with valid slug returns form detail with correct message")
    void getFormBySlug_withValidSlug_shouldReturnFormDetail() {
        savedForm.setAllowedDomains(List.of());
        savedForm.setQuestions(List.of());

        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));

        GetFormDetailResponse response = formService.getFormBySlug("test-form");

        assertThat(response.getMessage()).isEqualTo("Get form success");
        assertThat(response.getForm().getSlug()).isEqualTo("test-form");
        assertThat(response.getForm().getName()).isEqualTo("Test Form");
        assertThat(response.getForm().getCreatorId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getFormBySlug with no allowed_domains allows any authenticated user")
    void getFormBySlug_withNoAllowedDomains_shouldAllowAnyUser() {
        savedForm.setAllowedDomains(List.of());
        savedForm.setQuestions(List.of());

        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));

        assertThatCode(() -> formService.getFormBySlug("test-form")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getFormBySlug with matching allowed_domain allows access")
    void getFormBySlug_withAllowedDomain_userDomainMatches_shouldReturn() {
        savedForm.setAllowedDomains(List.of(domain(savedForm, "webtech.id")));
        savedForm.setQuestions(List.of());

        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));

        // user1@webtech.id's domain "webtech.id" matches — should succeed
        assertThatCode(() -> formService.getFormBySlug("test-form")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getFormBySlug with domain check is case-insensitive")
    void getFormBySlug_domainCheckIsCaseInsensitive() {
        savedForm.setAllowedDomains(List.of(domain(savedForm, "WEBTECH.ID")));
        savedForm.setQuestions(List.of());

        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));

        // "WEBTECH.ID" should match "webtech.id" (case-insensitive)
        assertThatCode(() -> formService.getFormBySlug("test-form")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getFormBySlug with forbidden domain throws ForbiddenAccessException")
    void getFormBySlug_withForbiddenDomain_shouldThrowForbiddenAccessException() {
        savedForm.setAllowedDomains(List.of(domain(savedForm, "other.org")));
        savedForm.setQuestions(List.of());

        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));

        assertThatThrownBy(() -> formService.getFormBySlug("test-form"))
                .isInstanceOf(ForbiddenAccessException.class);
    }

    @Test
    @DisplayName("getFormBySlug with unknown slug throws FormNotFoundException")
    void getFormBySlug_withUnknownSlug_shouldThrowFormNotFoundException() {
        when(formRepository.findBySlug("no-such-slug")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> formService.getFormBySlug("no-such-slug"))
                .isInstanceOf(FormNotFoundException.class);
    }

    @Test
    @DisplayName("getFormBySlug maps questions with choice_type as lowercase with spaces")
    void getFormBySlug_shouldMapQuestionsWithChoiceTypeAsLowercase() {
        Question q1 = question(savedForm, "Favorite stack", ChoiceType.SHORT_ANSWER, false);
        Question q2 = question(savedForm, "Preferred OS", ChoiceType.MULTIPLE_CHOICE, true);
        savedForm.setAllowedDomains(List.of());
        savedForm.setQuestions(List.of(q1, q2));

        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));

        GetFormDetailResponse response = formService.getFormBySlug("test-form");

        List<QuestionDto> questions = response.getForm().getQuestions();
        assertThat(questions).hasSize(2);
        assertThat(questions.get(0).getChoiceType()).isEqualTo("short answer");
        assertThat(questions.get(1).getChoiceType()).isEqualTo("multiple choice");
        assertThat(questions.get(1).isRequired()).isTrue();
        assertThat(questions.get(0).isRequired()).isFalse();
    }

    @Test
    @DisplayName("getFormBySlug includes allowed_domains list in response")
    void getFormBySlug_shouldIncludeAllowedDomainsInResponse() {
        savedForm.setAllowedDomains(List.of(
                domain(savedForm, "webtech.id"),
                domain(savedForm, "worldskills.org")
        ));
        savedForm.setQuestions(List.of());

        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));

        GetFormDetailResponse response = formService.getFormBySlug("test-form");

        assertThat(response.getForm().getAllowedDomains())
                .containsExactlyInAnyOrder("webtech.id", "worldskills.org");
    }

    @Test
    @DisplayName("getFormBySlug with null allowed_domains returns empty list in response")
    void getFormBySlug_withNullAllowedDomains_shouldReturnEmptyList() {
        savedForm.setAllowedDomains(null);
        savedForm.setQuestions(List.of());

        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));

        GetFormDetailResponse response = formService.getFormBySlug("test-form");

        assertThat(response.getForm().getAllowedDomains()).isEmpty();
    }

    @Test
    @DisplayName("getFormBySlug with null questions returns empty list in response")
    void getFormBySlug_withNullQuestions_shouldReturnEmptyList() {
        savedForm.setAllowedDomains(List.of());
        savedForm.setQuestions(null);

        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));

        GetFormDetailResponse response = formService.getFormBySlug("test-form");

        assertThat(response.getForm().getQuestions()).isEmpty();
    }

    // =========================================================================
    // addQuestion
    // =========================================================================

    private AddQuestionRequest buildAddQuestionRequest(String name, String choiceType,
                                                        List<String> choices, boolean isRequired) {
        AddQuestionRequest req = new AddQuestionRequest();
        ReflectionTestUtils.setField(req, "name", name);
        ReflectionTestUtils.setField(req, "choiceType", choiceType);
        ReflectionTestUtils.setField(req, "choices", choices);
        ReflectionTestUtils.setField(req, "isRequired", isRequired);
        return req;
    }

    private Question buildSavedQuestion(Long id, Form form, String name,
                                         ChoiceType type, String choices, boolean required) {
        Question q = new Question();
        q.setId(id);
        q.setForm(form);
        q.setName(name);
        q.setChoiceType(type);
        q.setChoices(choices);
        ReflectionTestUtils.setField(q, "isRequired", required);
        return q;
    }

    @Test
    @DisplayName("addQuestion returns AddQuestionResponse with message 'Add question success'")
    void addQuestion_shouldReturnCorrectMessage() {
        Question saved = buildSavedQuestion(1L, savedForm, "Q", ChoiceType.SHORT_ANSWER, null, false);
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));
        when(questionRepository.save(any(Question.class))).thenReturn(saved);

        AddQuestionRequest request = buildAddQuestionRequest("Q", "short answer", null, false);
        AddQuestionResponse response = formService.addQuestion("test-form", request);

        assertThat(response.getMessage()).isEqualTo("Add question success");
    }

    @Test
    @DisplayName("addQuestion response contains correct question fields")
    void addQuestion_shouldReturnCorrectQuestionFields() {
        Question saved = buildSavedQuestion(7L, savedForm, "Favorite Stack",
                ChoiceType.SHORT_ANSWER, null, true);
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));
        when(questionRepository.save(any(Question.class))).thenReturn(saved);

        AddQuestionRequest request = buildAddQuestionRequest("Favorite Stack", "short answer", null, true);
        AddQuestionResponse response = formService.addQuestion("test-form", request);

        QuestionDto dto = response.getQuestion();
        assertThat(dto.getId()).isEqualTo(7L);
        assertThat(dto.getName()).isEqualTo("Favorite Stack");
        assertThat(dto.getChoiceType()).isEqualTo("short answer");
        assertThat(dto.getFormId()).isEqualTo(10L);
        assertThat(dto.isRequired()).isTrue();
        assertThat(dto.getChoices()).isNull();
    }

    @Test
    @DisplayName("addQuestion joins choices list to comma-separated string on the saved entity")
    void addQuestion_shouldJoinChoicesToCommaSeparatedString() {
        List<String> choices = List.of("React", "Vue", "Angular");
        Question saved = buildSavedQuestion(2L, savedForm, "Framework",
                ChoiceType.MULTIPLE_CHOICE, "React,Vue,Angular", false);
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));
        when(questionRepository.save(any(Question.class))).thenReturn(saved);

        AddQuestionRequest request = buildAddQuestionRequest("Framework", "multiple choice", choices, false);
        formService.addQuestion("test-form", request);

        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(captor.capture());
        assertThat(captor.getValue().getChoices()).isEqualTo("React,Vue,Angular");
    }

    @Test
    @DisplayName("addQuestion response returns comma-separated string in choices field")
    void addQuestion_shouldReturnCommaSeparatedChoicesInResponse() {
        Question saved = buildSavedQuestion(3L, savedForm, "OS",
                ChoiceType.DROPDOWN, "Windows,macOS,Linux", true);
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));
        when(questionRepository.save(any(Question.class))).thenReturn(saved);

        AddQuestionRequest request = buildAddQuestionRequest("OS", "dropdown",
                List.of("Windows", "macOS", "Linux"), true);
        AddQuestionResponse response = formService.addQuestion("test-form", request);

        assertThat(response.getQuestion().getChoices()).isEqualTo("Windows,macOS,Linux");
    }

    @Test
    @DisplayName("addQuestion with null choices stores null on entity")
    void addQuestion_withNullChoices_shouldStoreNullOnEntity() {
        Question saved = buildSavedQuestion(4L, savedForm, "Name", ChoiceType.SHORT_ANSWER, null, false);
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));
        when(questionRepository.save(any(Question.class))).thenReturn(saved);

        AddQuestionRequest request = buildAddQuestionRequest("Name", "short answer", null, false);
        formService.addQuestion("test-form", request);

        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(captor.capture());
        assertThat(captor.getValue().getChoices()).isNull();
    }

    @Test
    @DisplayName("addQuestion with empty choices list stores null on entity")
    void addQuestion_withEmptyChoices_shouldStoreNullOnEntity() {
        Question saved = buildSavedQuestion(5L, savedForm, "Name", ChoiceType.SHORT_ANSWER, null, false);
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));
        when(questionRepository.save(any(Question.class))).thenReturn(saved);

        AddQuestionRequest request = buildAddQuestionRequest("Name", "short answer", List.of(), false);
        formService.addQuestion("test-form", request);

        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(captor.capture());
        assertThat(captor.getValue().getChoices()).isNull();
    }

    @Test
    @DisplayName("addQuestion sets is_required=true correctly on the saved entity")
    void addQuestion_withIsRequiredTrue_shouldSetTrueOnEntity() {
        Question saved = buildSavedQuestion(6L, savedForm, "Q", ChoiceType.SHORT_ANSWER, null, true);
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));
        when(questionRepository.save(any(Question.class))).thenReturn(saved);

        AddQuestionRequest request = buildAddQuestionRequest("Q", "short answer", null, true);
        formService.addQuestion("test-form", request);

        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(captor.capture());
        assertThat(captor.getValue().isRequired()).isTrue();
    }

    @Test
    @DisplayName("addQuestion sets form reference on the saved entity")
    void addQuestion_shouldSetFormOnEntity() {
        Question saved = buildSavedQuestion(8L, savedForm, "Q", ChoiceType.SHORT_ANSWER, null, false);
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));
        when(questionRepository.save(any(Question.class))).thenReturn(saved);

        AddQuestionRequest request = buildAddQuestionRequest("Q", "short answer", null, false);
        formService.addQuestion("test-form", request);

        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(captor.capture());
        assertThat(captor.getValue().getForm().getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("addQuestion converts display name to correct ChoiceType enum on entity")
    void addQuestion_shouldConvertChoiceTypeDisplayNameToEnum() {
        Question saved = buildSavedQuestion(9L, savedForm, "Pick one",
                ChoiceType.MULTIPLE_CHOICE, "A,B", false);
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));
        when(questionRepository.save(any(Question.class))).thenReturn(saved);

        AddQuestionRequest request = buildAddQuestionRequest("Pick one", "multiple choice",
                List.of("A", "B"), false);
        formService.addQuestion("test-form", request);

        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(captor.capture());
        assertThat(captor.getValue().getChoiceType()).isEqualTo(ChoiceType.MULTIPLE_CHOICE);
    }

    @Test
    @DisplayName("addQuestion serializes choice_type enum back to display name in response")
    void addQuestion_shouldSerializeChoiceTypeToDisplayNameInResponse() {
        Question saved = buildSavedQuestion(10L, savedForm, "Q",
                ChoiceType.CHECKBOXES, "X,Y", false);
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));
        when(questionRepository.save(any(Question.class))).thenReturn(saved);

        AddQuestionRequest request = buildAddQuestionRequest("Q", "checkboxes",
                List.of("X", "Y"), false);
        AddQuestionResponse response = formService.addQuestion("test-form", request);

        assertThat(response.getQuestion().getChoiceType()).isEqualTo("checkboxes");
    }

    @Test
    @DisplayName("addQuestion with unknown slug throws FormNotFoundException")
    void addQuestion_withUnknownSlug_shouldThrowFormNotFoundException() {
        when(formRepository.findBySlug("no-such-form")).thenReturn(Optional.empty());

        AddQuestionRequest request = buildAddQuestionRequest("Q", "short answer", null, false);

        assertThatThrownBy(() -> formService.addQuestion("no-such-form", request))
                .isInstanceOf(FormNotFoundException.class);
    }

    @Test
    @DisplayName("addQuestion by non-creator throws ForbiddenAccessException")
    void addQuestion_byNonCreator_shouldThrowForbiddenAccessException() {
        // Override authentication: user2 is not the creator of savedForm
        var auth = new UsernamePasswordAuthenticationToken("user2@other.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));

        AddQuestionRequest request = buildAddQuestionRequest("Q", "short answer", null, false);

        assertThatThrownBy(() -> formService.addQuestion("test-form", request))
                .isInstanceOf(ForbiddenAccessException.class);
    }

    @Test
    @DisplayName("addQuestion by non-creator does not save any question")
    void addQuestion_byNonCreator_shouldNotSaveQuestion() {
        var auth = new UsernamePasswordAuthenticationToken("user2@other.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));

        AddQuestionRequest request = buildAddQuestionRequest("Q", "short answer", null, false);

        assertThatThrownBy(() -> formService.addQuestion("test-form", request))
                .isInstanceOf(ForbiddenAccessException.class);
        verify(questionRepository, never()).save(any());
    }

    // =========================================================================
    // removeQuestion
    // =========================================================================

    private Question existingQuestion(Long id, Form form) {
        Question q = new Question();
        q.setId(id);
        q.setForm(form);
        q.setName("Existing Question");
        q.setChoiceType(ChoiceType.SHORT_ANSWER);
        ReflectionTestUtils.setField(q, "isRequired", false);
        return q;
    }

    @Test
    @DisplayName("removeQuestion returns message 'Remove question success'")
    void removeQuestion_shouldReturnSuccessMessage() {
        Question q = existingQuestion(5L, savedForm);
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));
        when(questionRepository.findByIdAndForm(5L, savedForm)).thenReturn(Optional.of(q));

        MessageResponse response = formService.removeQuestion("test-form", 5L);

        assertThat(response.getMessage()).isEqualTo("Remove question success");
    }

    @Test
    @DisplayName("removeQuestion calls questionRepository.delete with the correct question")
    void removeQuestion_shouldDeleteTheQuestion() {
        Question q = existingQuestion(5L, savedForm);
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));
        when(questionRepository.findByIdAndForm(5L, savedForm)).thenReturn(Optional.of(q));

        formService.removeQuestion("test-form", 5L);

        verify(questionRepository).delete(q);
    }

    @Test
    @DisplayName("removeQuestion with unknown form slug throws FormNotFoundException")
    void removeQuestion_withUnknownSlug_shouldThrowFormNotFoundException() {
        when(formRepository.findBySlug("no-form")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> formService.removeQuestion("no-form", 1L))
                .isInstanceOf(FormNotFoundException.class);
    }

    @Test
    @DisplayName("removeQuestion with unknown question id throws QuestionNotFoundException")
    void removeQuestion_withUnknownQuestionId_shouldThrowQuestionNotFoundException() {
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));
        when(questionRepository.findByIdAndForm(99L, savedForm)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> formService.removeQuestion("test-form", 99L))
                .isInstanceOf(QuestionNotFoundException.class);
    }

    @Test
    @DisplayName("removeQuestion where question belongs to a different form throws QuestionNotFoundException")
    void removeQuestion_questionFromDifferentForm_shouldThrowQuestionNotFoundException() {
        // question id 5 does not belong to savedForm — findByIdAndForm returns empty
        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));
        when(questionRepository.findByIdAndForm(5L, savedForm)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> formService.removeQuestion("test-form", 5L))
                .isInstanceOf(QuestionNotFoundException.class);
    }

    @Test
    @DisplayName("removeQuestion by non-creator throws ForbiddenAccessException")
    void removeQuestion_byNonCreator_shouldThrowForbiddenAccessException() {
        var auth = new UsernamePasswordAuthenticationToken("user2@other.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));

        assertThatThrownBy(() -> formService.removeQuestion("test-form", 5L))
                .isInstanceOf(ForbiddenAccessException.class);
    }

    @Test
    @DisplayName("removeQuestion by non-creator does not delete any question")
    void removeQuestion_byNonCreator_shouldNotDeleteQuestion() {
        var auth = new UsernamePasswordAuthenticationToken("user2@other.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(formRepository.findBySlug("test-form")).thenReturn(Optional.of(savedForm));

        assertThatThrownBy(() -> formService.removeQuestion("test-form", 5L))
                .isInstanceOf(ForbiddenAccessException.class);
        verify(questionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("removeQuestion with unknown form does not attempt question lookup")
    void removeQuestion_withUnknownForm_shouldNotQueryQuestion() {
        when(formRepository.findBySlug("no-form")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> formService.removeQuestion("no-form", 1L))
                .isInstanceOf(FormNotFoundException.class);
        verify(questionRepository, never()).findByIdAndForm(any(), any());
    }
}
