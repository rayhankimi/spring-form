package com.rayhank.tech_assesment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayhank.tech_assesment.entity.*;
import com.rayhank.tech_assesment.repository.*;
import com.rayhank.tech_assesment.security.JwtService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class ResponseControllerTest {

    private static final String FORMS_URL = "/api/v1/forms";

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private JwtService jwtService;
    @Autowired private UserDetailsService userDetailsService;
    @Autowired private FormRepository formRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private ResponseRepository responseRepository;
    @Autowired private EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;
    private String user1Token; // user1@webtech.id  (creator, webtech.id domain)
    private String user2Token; // user2@webtech.id  (same domain)
    private String user3Token; // user3@worldskills.org (different domain)

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        user1Token = jwtService.generateToken(userDetailsService.loadUserByUsername("user1@webtech.id"));
        user2Token = jwtService.generateToken(userDetailsService.loadUserByUsername("user2@webtech.id"));
        user3Token = jwtService.generateToken(userDetailsService.loadUserByUsername("user3@worldskills.org"));
    }

    private String uniqueSlug(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private Form seedForm(User owner, String slug, boolean limitOneResponse, String... domains) {
        Form form = new Form();
        form.setName("Form " + slug);
        form.setSlug(slug);
        form.setLimitOneResponse(limitOneResponse);
        form.setCreator(owner);

        if (domains.length > 0) {
            List<AllowedDomain> ads = new java.util.ArrayList<>();
            for (String d : domains) {
                AllowedDomain ad = new AllowedDomain();
                ad.setForm(form);
                ad.setDomain(d);
                ads.add(ad);
            }
            form.setAllowedDomains(ads);
        }
        return formRepository.save(form);
    }

    private Question seedQuestion(Form form, String name, boolean required) {
        Question q = new Question();
        q.setForm(form);
        q.setName(name);
        q.setChoiceType(ChoiceType.SHORT_ANSWER);
        org.springframework.test.util.ReflectionTestUtils.setField(q, "isRequired", required);
        return questionRepository.save(q);
    }

    // =========================================================================
    // POST /api/v1/forms/{slug}/responses — Submit Response
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/forms/{slug}/responses")
    class SubmitResponse {

        @Test
        @DisplayName("200 — valid submission with answers returns 'Submit response success'")
        @Transactional
        void validRequest_shouldReturn200() throws Exception {
            User user1 = userRepository.findByEmail("user1@webtech.id").orElseThrow();
            String slug = uniqueSlug("sr-ok");
            Form form = seedForm(user1, slug, false);
            Question q = seedQuestion(form, "Name", false);

            entityManager.flush();
            entityManager.clear();

            var body = Map.of("answers", List.of(
                    Map.of("question_id", q.getId(), "value", "Alice")
            ));

            mockMvc.perform(post(FORMS_URL + "/" + slug + "/responses")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Submit response success"));
        }

        @Test
        @DisplayName("200 — form with no allowed_domains accepts any authenticated user")
        @Transactional
        void openForm_anyUserCanSubmit() throws Exception {
            User user1 = userRepository.findByEmail("user1@webtech.id").orElseThrow();
            String slug = uniqueSlug("sr-open");
            seedForm(user1, slug, false); // no domains = open

            entityManager.flush();
            entityManager.clear();

            var body = Map.of("answers", List.of());

            // user3 (worldskills.org) can submit to open form
            mockMvc.perform(post(FORMS_URL + "/" + slug + "/responses")
                            .header("Authorization", "Bearer " + user3Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("200 — required question answered allows submission")
        @Transactional
        void requiredQuestionAnswered_shouldReturn200() throws Exception {
            User user1 = userRepository.findByEmail("user1@webtech.id").orElseThrow();
            String slug = uniqueSlug("sr-req");
            Form form = seedForm(user1, slug, false);
            Question q = seedQuestion(form, "Name", true); // required

            entityManager.flush();
            entityManager.clear();

            var body = Map.of("answers", List.of(
                    Map.of("question_id", q.getId(), "value", "Alice")
            ));

            mockMvc.perform(post(FORMS_URL + "/" + slug + "/responses")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Submit response success"));
        }

        // --- 422 Validation ---

        @Test
        @DisplayName("422 — missing answers field returns error on answers")
        @Transactional
        void missingAnswers_shouldReturn422() throws Exception {
            User user1 = userRepository.findByEmail("user1@webtech.id").orElseThrow();
            String slug = uniqueSlug("sr-noans");
            seedForm(user1, slug, false);

            mockMvc.perform(post(FORMS_URL + "/" + slug + "/responses")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Invalid field"))
                    .andExpect(jsonPath("$.errors.answers").isArray())
                    .andExpect(jsonPath("$.errors.answers[0]").value("The answers field is required."));
        }

        @Test
        @DisplayName("422 — required question not answered returns error on that answer field")
        @Transactional
        void requiredQuestionNotAnswered_shouldReturn422() throws Exception {
            User user1 = userRepository.findByEmail("user1@webtech.id").orElseThrow();
            String slug = uniqueSlug("sr-misreq");
            Form form = seedForm(user1, slug, false);
            seedQuestion(form, "Name", true); // required, no answer submitted

            entityManager.flush();
            entityManager.clear();

            var body = Map.of("answers", List.of()); // empty answers

            mockMvc.perform(post(FORMS_URL + "/" + slug + "/responses")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Invalid field"))
                    .andExpect(jsonPath("$.errors").isMap());
        }

        @Test
        @DisplayName("422 — submitting twice when limit_one_response returns proper message")
        @Transactional
        void submitTwice_withLimitOneResponse_shouldReturn422() throws Exception {
            User user1 = userRepository.findByEmail("user1@webtech.id").orElseThrow();
            String slug = uniqueSlug("sr-limit");
            seedForm(user1, slug, true); // limit_one_response = true

            entityManager.flush();
            entityManager.clear();

            var body = Map.of("answers", List.of());

            // First submission succeeds
            mockMvc.perform(post(FORMS_URL + "/" + slug + "/responses")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());

            // Second submission is rejected
            mockMvc.perform(post(FORMS_URL + "/" + slug + "/responses")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("You can not submit form twice"));
        }

        @Test
        @DisplayName("422 — submitting twice when limit_one_response=false is allowed")
        @Transactional
        void submitTwice_withoutLimit_shouldReturn200Both() throws Exception {
            User user1 = userRepository.findByEmail("user1@webtech.id").orElseThrow();
            String slug = uniqueSlug("sr-nolimit");
            seedForm(user1, slug, false); // no limit

            entityManager.flush();
            entityManager.clear();

            var body = Map.of("answers", List.of());

            mockMvc.perform(post(FORMS_URL + "/" + slug + "/responses")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());

            mockMvc.perform(post(FORMS_URL + "/" + slug + "/responses")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }

        // --- 403 Domain Restriction ---

        @Test
        @DisplayName("403 — user with forbidden domain cannot submit")
        @Transactional
        void forbiddenDomain_shouldReturn403() throws Exception {
            User user1 = userRepository.findByEmail("user1@webtech.id").orElseThrow();
            String slug = uniqueSlug("sr-dom");
            seedForm(user1, slug, false, "webtech.id"); // only webtech.id allowed

            entityManager.flush();
            entityManager.clear();

            var body = Map.of("answers", List.of());

            // user3 is worldskills.org — forbidden
            mockMvc.perform(post(FORMS_URL + "/" + slug + "/responses")
                            .header("Authorization", "Bearer " + user3Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Forbidden access"));
        }

        @Test
        @DisplayName("403 — user with allowed domain can submit")
        @Transactional
        void allowedDomain_shouldReturn200() throws Exception {
            User user1 = userRepository.findByEmail("user1@webtech.id").orElseThrow();
            String slug = uniqueSlug("sr-dom-ok");
            seedForm(user1, slug, false, "webtech.id");

            entityManager.flush();
            entityManager.clear();

            var body = Map.of("answers", List.of());

            // user2 is webtech.id — allowed
            mockMvc.perform(post(FORMS_URL + "/" + slug + "/responses")
                            .header("Authorization", "Bearer " + user2Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }

        // --- 404 Form Not Found ---

        @Test
        @DisplayName("404 — unknown form slug returns 'Form not found'")
        void unknownFormSlug_shouldReturn404() throws Exception {
            var body = Map.of("answers", List.of());

            mockMvc.perform(post(FORMS_URL + "/no-such-form/responses")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Form not found"));
        }

        // --- 401 Unauthenticated ---

        @Test
        @DisplayName("401 — request without token returns 'Unauthenticated.'")
        void noToken_shouldReturn401() throws Exception {
            mockMvc.perform(post(FORMS_URL + "/any-slug/responses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"answers\":[]}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Unauthenticated."));
        }

        @Test
        @DisplayName("401 — request with invalid token returns 'Unauthenticated.'")
        void invalidToken_shouldReturn401() throws Exception {
            mockMvc.perform(post(FORMS_URL + "/any-slug/responses")
                            .header("Authorization", "Bearer bad.token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"answers\":[]}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Unauthenticated."));
        }
    }

    // =========================================================================
    // GET /api/v1/forms/{slug}/responses — Get All Responses
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/forms/{slug}/responses")
    class GetResponses {

        @Test
        @DisplayName("200 — creator gets responses with correct message")
        @Transactional
        void creator_shouldReturn200WithResponses() throws Exception {
            User user1 = userRepository.findByEmail("user1@webtech.id").orElseThrow();
            String slug = uniqueSlug("gr-ok");
            seedForm(user1, slug, false);

            mockMvc.perform(get(FORMS_URL + "/" + slug + "/responses")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Get responses success"))
                    .andExpect(jsonPath("$.responses").isArray());
        }

        @Test
        @DisplayName("200 — response entries contain date, user, and answers fields")
        @Transactional
        void responseEntry_shouldContainRequiredFields() throws Exception {
            User user1 = userRepository.findByEmail("user1@webtech.id").orElseThrow();
            User user2 = userRepository.findByEmail("user2@webtech.id").orElseThrow();
            String slug = uniqueSlug("gr-fields");
            Form form = seedForm(user1, slug, false);
            Question q = seedQuestion(form, "Name", false);

            entityManager.flush();
            entityManager.clear();

            // user2 submits a response
            var submitBody = Map.of("answers", List.of(
                    Map.of("question_id", q.getId(), "value", "Bob")
            ));
            mockMvc.perform(post(FORMS_URL + "/" + slug + "/responses")
                            .header("Authorization", "Bearer " + user2Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(submitBody)))
                    .andExpect(status().isOk());

            // creator retrieves responses
            mockMvc.perform(get(FORMS_URL + "/" + slug + "/responses")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.responses", hasSize(1)))
                    .andExpect(jsonPath("$.responses[0].date").exists())
                    .andExpect(jsonPath("$.responses[0].user.id").isNumber())
                    .andExpect(jsonPath("$.responses[0].user.name").value("User 2"))
                    .andExpect(jsonPath("$.responses[0].user.email").value("user2@webtech.id"))
                    .andExpect(jsonPath("$.responses[0].user.email_verified_at").isEmpty())
                    .andExpect(jsonPath("$.responses[0].answers.Name").value("Bob"));
        }

        @Test
        @DisplayName("200 — answers map uses question name as key and submitted value as value")
        @Transactional
        void answersMap_shouldUseQuestionNameAsKey() throws Exception {
            User user1 = userRepository.findByEmail("user1@webtech.id").orElseThrow();
            String slug = uniqueSlug("gr-ansmap");
            Form form = seedForm(user1, slug, false);
            Question q1 = seedQuestion(form, "Full Name", false);
            Question q2 = seedQuestion(form, "City", false);

            entityManager.flush();
            entityManager.clear();

            var body = Map.of("answers", List.of(
                    Map.of("question_id", q1.getId(), "value", "Alice"),
                    Map.of("question_id", q2.getId(), "value", "Jakarta")
            ));
            mockMvc.perform(post(FORMS_URL + "/" + slug + "/responses")
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());

            mockMvc.perform(get(FORMS_URL + "/" + slug + "/responses")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.responses[0].answers['Full Name']").value("Alice"))
                    .andExpect(jsonPath("$.responses[0].answers.City").value("Jakarta"));
        }

        @Test
        @DisplayName("200 — empty responses list is returned when no submissions exist")
        @Transactional
        void noResponses_shouldReturnEmptyList() throws Exception {
            User user1 = userRepository.findByEmail("user1@webtech.id").orElseThrow();
            String slug = uniqueSlug("gr-empty");
            seedForm(user1, slug, false);

            mockMvc.perform(get(FORMS_URL + "/" + slug + "/responses")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.responses", hasSize(0)));
        }

        // --- 403 Forbidden ---

        @Test
        @DisplayName("403 — non-creator accessing responses returns 'Forbidden access'")
        @Transactional
        void nonCreator_shouldReturn403() throws Exception {
            User user1 = userRepository.findByEmail("user1@webtech.id").orElseThrow();
            String slug = uniqueSlug("gr-forbidden");
            seedForm(user1, slug, false);

            // user2 is not the creator
            mockMvc.perform(get(FORMS_URL + "/" + slug + "/responses")
                            .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Forbidden access"));
        }

        // --- 404 Form Not Found ---

        @Test
        @DisplayName("404 — unknown form slug returns 'Form not found'")
        void unknownFormSlug_shouldReturn404() throws Exception {
            mockMvc.perform(get(FORMS_URL + "/no-such-form/responses")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Form not found"));
        }

        // --- 401 Unauthenticated ---

        @Test
        @DisplayName("401 — request without token returns 'Unauthenticated.'")
        void noToken_shouldReturn401() throws Exception {
            mockMvc.perform(get(FORMS_URL + "/any-slug/responses"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Unauthenticated."));
        }

        @Test
        @DisplayName("401 — request with invalid token returns 'Unauthenticated.'")
        void invalidToken_shouldReturn401() throws Exception {
            mockMvc.perform(get(FORMS_URL + "/any-slug/responses")
                            .header("Authorization", "Bearer bad.token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Unauthenticated."));
        }
    }
}
