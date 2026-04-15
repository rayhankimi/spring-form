package com.rayhank.tech_assesment.service;

import com.rayhank.tech_assesment.dto.form.*;
import com.rayhank.tech_assesment.entity.*;
import com.rayhank.tech_assesment.exception.ForbiddenAccessException;
import com.rayhank.tech_assesment.exception.FormNotFoundException;
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
}
