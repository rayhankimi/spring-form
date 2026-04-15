package com.rayhank.tech_assesment.service;

import com.rayhank.tech_assesment.dto.form.CreateFormRequest;
import com.rayhank.tech_assesment.dto.form.CreateFormResponse;
import com.rayhank.tech_assesment.entity.Form;
import com.rayhank.tech_assesment.entity.User;
import com.rayhank.tech_assesment.repository.FormRepository;
import com.rayhank.tech_assesment.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormServiceTest {

    @Mock private FormRepository formRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private FormService formService;

    private User creator;
    private Form savedForm;

    @BeforeEach
    void setUp() {
        // Put a real Authentication into the context so SecurityContextHolder.getContext().getAuthentication().getName() works
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
                .stream().map(d -> d.getDomain()).toList();

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
}
