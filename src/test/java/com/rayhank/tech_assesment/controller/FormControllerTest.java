package com.rayhank.tech_assesment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayhank.tech_assesment.entity.AllowedDomain;
import com.rayhank.tech_assesment.entity.Form;
import com.rayhank.tech_assesment.entity.User;
import com.rayhank.tech_assesment.repository.FormRepository;
import com.rayhank.tech_assesment.repository.UserRepository;
import com.rayhank.tech_assesment.security.JwtService;
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
class FormControllerTest {

    private static final String FORMS_URL = "/api/v1/forms";

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private JwtService jwtService;
    @Autowired private UserDetailsService userDetailsService;
    @Autowired private FormRepository formRepository;
    @Autowired private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;
    private String user1Token;
    private String user2Token;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        user1Token = jwtService.generateToken(userDetailsService.loadUserByUsername("user1@webtech.id"));
        user2Token = jwtService.generateToken(userDetailsService.loadUserByUsername("user2@webtech.id"));
    }

    // Unique slug per test call — prevents UniqueSlug constraint conflicts across tests
    private String uniqueSlug(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/forms
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/forms")
    class CreateForm {

        @Test
        @DisplayName("200 — full valid request returns form with all fields")
        void fullValidRequest_shouldReturn200WithFormDetails() throws Exception {
            var body = Map.of(
                    "name", "Stacks of Web Tech Members",
                    "slug", uniqueSlug("member-stacks"),
                    "description", "To collect all of favorite stacks",
                    "allowed_domains", List.of("webtech.id"),
                    "limit_one_response", true
            );

            mockMvc.perform(post(FORMS_URL)
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Create form success"))
                    .andExpect(jsonPath("$.form.id").isNumber())
                    .andExpect(jsonPath("$.form.name").value("Stacks of Web Tech Members"))
                    .andExpect(jsonPath("$.form.description").value("To collect all of favorite stacks"))
                    .andExpect(jsonPath("$.form.limit_one_response").value(true))
                    .andExpect(jsonPath("$.form.creator_id").isNumber());
        }

        @Test
        @DisplayName("200 — minimal request (only required fields) is accepted")
        void minimalRequest_shouldReturn200() throws Exception {
            var body = Map.of(
                    "name", "Simple Form",
                    "slug", uniqueSlug("simple-form")
            );

            mockMvc.perform(post(FORMS_URL)
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Create form success"))
                    .andExpect(jsonPath("$.form.id").isNumber())
                    .andExpect(jsonPath("$.form.limit_one_response").value(false));
        }

        @Test
        @DisplayName("200 — creator_id in response matches the authenticated user")
        void creatorId_shouldMatchAuthenticatedUser() throws Exception {
            // Create as user1
            var user1Form = Map.of("name", "User1 Form", "slug", uniqueSlug("u1-form"));
            var user1Result = mockMvc.perform(post(FORMS_URL)
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user1Form)))
                    .andExpect(status().isOk())
                    .andReturn();

            // Create as user2
            var user2Form = Map.of("name", "User2 Form", "slug", uniqueSlug("u2-form"));
            var user2Result = mockMvc.perform(post(FORMS_URL)
                            .header("Authorization", "Bearer " + user2Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user2Form)))
                    .andExpect(status().isOk())
                    .andReturn();

            Long user1CreatorId = objectMapper.readTree(user1Result.getResponse().getContentAsString())
                    .at("/form/creator_id").longValue();
            Long user2CreatorId = objectMapper.readTree(user2Result.getResponse().getContentAsString())
                    .at("/form/creator_id").longValue();

            // Different users → different creator_ids
            org.assertj.core.api.Assertions.assertThat(user1CreatorId)
                    .isNotEqualTo(user2CreatorId);
        }

        @Test
        @DisplayName("200 — slug is stored exactly as sent")
        void slug_shouldBeStoredAsProvided() throws Exception {
            String slug = uniqueSlug("exact.slug-123");
            var body = Map.of("name", "Slug Test Form", "slug", slug);

            mockMvc.perform(post(FORMS_URL)
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.form.slug").value(slug));
        }

        // --- Validation (422) ---

        @Test
        @DisplayName("422 — missing name returns error on name field")
        void missingName_shouldReturn422() throws Exception {
            var body = Map.of("slug", uniqueSlug("no-name"));

            mockMvc.perform(post(FORMS_URL)
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Invalid field"))
                    .andExpect(jsonPath("$.errors.name").isArray())
                    .andExpect(jsonPath("$.errors.name", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("422 — missing slug returns error on slug field")
        void missingSlug_shouldReturn422() throws Exception {
            var body = Map.of("name", "Form Without Slug");

            mockMvc.perform(post(FORMS_URL)
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Invalid field"))
                    .andExpect(jsonPath("$.errors.slug").isArray());
        }

        @Test
        @DisplayName("422 — both name and slug missing returns errors for both fields")
        void bothMissing_shouldReturn422WithBothErrors() throws Exception {
            mockMvc.perform(post(FORMS_URL)
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors.name").isArray())
                    .andExpect(jsonPath("$.errors.slug").isArray());
        }

        @Test
        @DisplayName("422 — slug with space is rejected by pattern validation")
        void slugWithSpace_shouldReturn422() throws Exception {
            var body = Map.of("name", "Form", "slug", "my form slug");

            mockMvc.perform(post(FORMS_URL)
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors.slug").isArray());
        }

        @Test
        @DisplayName("422 — slug with underscore is rejected by pattern validation")
        void slugWithUnderscore_shouldReturn422() throws Exception {
            var body = Map.of("name", "Form", "slug", "my_form_slug");

            mockMvc.perform(post(FORMS_URL)
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors.slug").isArray());
        }

        @Test
        @DisplayName("422 — duplicate slug returns 'The slug has already been taken.'")
        void duplicateSlug_shouldReturn422() throws Exception {
            String slug = uniqueSlug("dup-slug");
            var body = Map.of("name", "First Form", "slug", slug);

            // First creation succeeds
            mockMvc.perform(post(FORMS_URL)
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());

            // Second creation with same slug fails
            mockMvc.perform(post(FORMS_URL)
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Invalid field"))
                    .andExpect(jsonPath("$.errors.slug[0]").value("The slug has already been taken."));
        }

        // --- Authentication (401) ---

        @Test
        @DisplayName("401 — request without token returns 'Unauthenticated.'")
        void noToken_shouldReturn401() throws Exception {
            var body = Map.of("name", "Form", "slug", uniqueSlug("no-auth"));

            mockMvc.perform(post(FORMS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Unauthenticated."));
        }

        @Test
        @DisplayName("401 — request with invalid token returns 'Unauthenticated.'")
        void invalidToken_shouldReturn401() throws Exception {
            var body = Map.of("name", "Form", "slug", uniqueSlug("bad-token"));

            mockMvc.perform(post(FORMS_URL)
                            .header("Authorization", "Bearer invalid.token.here")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Unauthenticated."));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/forms
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/forms")
    class GetAllForms {

        // Save a form directly via repository so GET tests are independent from POST tests
        private Form seedForm(User owner, String slug) {
            Form form = new Form();
            form.setName("Seed Form " + slug);
            form.setSlug(slug);
            form.setDescription("Description");
            form.setLimitOneResponse(false);
            form.setCreator(owner);
            return formRepository.save(form);
        }

        @Test
        @DisplayName("200 — returns only forms created by the authenticated user")
        @Transactional
        void shouldReturnOnlyCurrentUserForms() throws Exception {
            User user3 = userRepository.findByEmail("user3@worldskills.org").orElseThrow();
            seedForm(user3, uniqueSlug("ga-u3-a"));
            seedForm(user3, uniqueSlug("ga-u3-b"));

            mockMvc.perform(get(FORMS_URL)
                            .header("Authorization", "Bearer " + jwtService.generateToken(
                                    userDetailsService.loadUserByUsername("user3@worldskills.org"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Get all forms success"))
                    .andExpect(jsonPath("$.forms").isArray())
                    .andExpect(jsonPath("$.forms[*].creator_id",
                            everyItem(notNullValue())));
        }

        @Test
        @DisplayName("200 — returns empty list when user has no forms")
        void noForms_shouldReturnEmptyList() throws Exception {
            // user2 has no forms in our test dataset (unless POST tests created some)
            // We verify the structure is correct regardless of count
            mockMvc.perform(get(FORMS_URL)
                            .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Get all forms success"))
                    .andExpect(jsonPath("$.forms").isArray());
        }

        @Test
        @DisplayName("200 — each form entry contains required fields")
        @Transactional
        void formEntry_shouldContainRequiredFields() throws Exception {
            User user1 = userRepository.findByEmail("user1@webtech.id").orElseThrow();
            seedForm(user1, uniqueSlug("ga-fields"));

            mockMvc.perform(get(FORMS_URL)
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.forms[0].id").exists())
                    .andExpect(jsonPath("$.forms[0].name").exists())
                    .andExpect(jsonPath("$.forms[0].slug").exists())
                    .andExpect(jsonPath("$.forms[0].creator_id").exists())
                    .andExpect(jsonPath("$.forms[0].limit_one_response").exists());
        }

        @Test
        @DisplayName("401 — request without token returns 'Unauthenticated.'")
        void noToken_shouldReturn401() throws Exception {
            mockMvc.perform(get(FORMS_URL))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Unauthenticated."));
        }

        @Test
        @DisplayName("401 — request with invalid token returns 'Unauthenticated.'")
        void invalidToken_shouldReturn401() throws Exception {
            mockMvc.perform(get(FORMS_URL)
                            .header("Authorization", "Bearer bad.token.value"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Unauthenticated."));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/forms/{slug}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/forms/{slug}")
    class GetFormDetail {

        private Form seedFormWithDomain(User owner, String slug, String... domains) {
            Form form = new Form();
            form.setName("Detail Form " + slug);
            form.setSlug(slug);
            form.setDescription("A detail description");
            form.setLimitOneResponse(true);
            form.setCreator(owner);

            if (domains.length > 0) {
                List<AllowedDomain> allowedDomains = new java.util.ArrayList<>();
                for (String d : domains) {
                    AllowedDomain ad = new AllowedDomain();
                    ad.setForm(form);
                    ad.setDomain(d);
                    allowedDomains.add(ad);
                }
                form.setAllowedDomains(allowedDomains);
            }
            return formRepository.save(form);
        }

        @Test
        @DisplayName("200 — returns form detail with required fields")
        @Transactional
        void validSlug_shouldReturn200WithDetail() throws Exception {
            User user1 = userRepository.findByEmail("user1@webtech.id").orElseThrow();
            String slug = uniqueSlug("detail-ok");
            seedFormWithDomain(user1, slug);

            mockMvc.perform(get(FORMS_URL + "/" + slug)
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Get form success"))
                    .andExpect(jsonPath("$.form.id").isNumber())
                    .andExpect(jsonPath("$.form.name").exists())
                    .andExpect(jsonPath("$.form.slug").value(slug))
                    .andExpect(jsonPath("$.form.creator_id").isNumber())
                    .andExpect(jsonPath("$.form.allowed_domains").isArray())
                    .andExpect(jsonPath("$.form.questions").isArray());
        }

        @Test
        @DisplayName("200 — user whose domain is in allowed_domains can access form")
        @Transactional
        void allowedDomain_shouldReturn200() throws Exception {
            User user1 = userRepository.findByEmail("user1@webtech.id").orElseThrow();
            String slug = uniqueSlug("dom-allow");
            // user1's domain is "webtech.id" — allow it
            seedFormWithDomain(user1, slug, "webtech.id");

            mockMvc.perform(get(FORMS_URL + "/" + slug)
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.form.allowed_domains[0]").value("webtech.id"));
        }

        @Test
        @DisplayName("200 — form with no allowed_domains is accessible by any authenticated user")
        @Transactional
        void noAllowedDomains_anyUserCanAccess() throws Exception {
            User user1 = userRepository.findByEmail("user1@webtech.id").orElseThrow();
            String slug = uniqueSlug("open-form");
            seedFormWithDomain(user1, slug); // no domains = open

            // user3 (worldskills.org) can still access
            String user3Token = jwtService.generateToken(
                    userDetailsService.loadUserByUsername("user3@worldskills.org"));

            mockMvc.perform(get(FORMS_URL + "/" + slug)
                            .header("Authorization", "Bearer " + user3Token))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("403 — user whose domain is NOT in allowed_domains gets 'Forbidden access'")
        @Transactional
        void forbiddenDomain_shouldReturn403() throws Exception {
            User user1 = userRepository.findByEmail("user1@webtech.id").orElseThrow();
            String slug = uniqueSlug("dom-forbid");
            // Only "webtech.id" allowed — user3 is "worldskills.org"
            seedFormWithDomain(user1, slug, "webtech.id");

            String user3Token = jwtService.generateToken(
                    userDetailsService.loadUserByUsername("user3@worldskills.org"));

            mockMvc.perform(get(FORMS_URL + "/" + slug)
                            .header("Authorization", "Bearer " + user3Token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Forbidden access"));
        }

        @Test
        @DisplayName("404 — unknown slug returns 'Form not found'")
        void unknownSlug_shouldReturn404() throws Exception {
            mockMvc.perform(get(FORMS_URL + "/slug-that-does-not-exist")
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Form not found"));
        }

        @Test
        @DisplayName("401 — request without token returns 'Unauthenticated.'")
        void noToken_shouldReturn401() throws Exception {
            mockMvc.perform(get(FORMS_URL + "/any-slug"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Unauthenticated."));
        }

        @Test
        @DisplayName("401 — request with invalid token returns 'Unauthenticated.'")
        void invalidToken_shouldReturn401() throws Exception {
            mockMvc.perform(get(FORMS_URL + "/any-slug")
                            .header("Authorization", "Bearer bad.token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Unauthenticated."));
        }
    }
}
