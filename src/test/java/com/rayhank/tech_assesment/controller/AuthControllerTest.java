package com.rayhank.tech_assesment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayhank.tech_assesment.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AuthControllerTest {

    private static final String LOGIN_URL  = "/api/v1/auth/login";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private JwtService jwtService;
    @Autowired private UserDetailsService userDetailsService;

    // Instantiated directly — ObjectMapper is not exposed as a bean in Spring Boot 4 by default
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;
    private String validToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        var userDetails = userDetailsService.loadUserByUsername("user1@webtech.id");
        validToken = jwtService.generateToken(userDetails);
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/auth/login
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("200 — valid credentials return token and user info")
        void validCredentials_shouldReturn200WithToken() throws Exception {
            var body = Map.of("email", "user1@webtech.id", "password", "password1");

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Login success"))
                    .andExpect(jsonPath("$.user.name").value("User 1"))
                    .andExpect(jsonPath("$.user.email").value("user1@webtech.id"))
                    .andExpect(jsonPath("$.user.accessToken").isNotEmpty());
        }

        @Test
        @DisplayName("200 — all three seeded users can log in")
        void allSeededUsers_shouldReturn200() throws Exception {
            String[][] users = {
                    {"user1@webtech.id",      "password1", "User 1"},
                    {"user2@webtech.id",      "password2", "User 2"},
                    {"user3@worldskills.org", "password3", "User 3"},
            };

            for (String[] u : users) {
                var body = Map.of("email", u[0], "password", u[1]);
                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.user.email").value(u[0]))
                        .andExpect(jsonPath("$.user.name").value(u[2]));
            }
        }

        @Test
        @DisplayName("401 — wrong password returns 'Email or password incorrect'")
        void wrongPassword_shouldReturn401() throws Exception {
            var body = Map.of("email", "user1@webtech.id", "password", "wrongpass");

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Email or password incorrect"));
        }

        @Test
        @DisplayName("401 — non-existent email returns 'Email or password incorrect'")
        void unknownEmail_shouldReturn401() throws Exception {
            var body = Map.of("email", "ghost@webtech.id", "password", "password1");

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Email or password incorrect"));
        }

        @Test
        @DisplayName("422 — invalid email format returns field error")
        void invalidEmailFormat_shouldReturn422() throws Exception {
            var body = Map.of("email", "not-an-email", "password", "password1");

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Invalid field"))
                    .andExpect(jsonPath("$.errors.email").isArray())
                    .andExpect(jsonPath("$.errors.email", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("422 — missing email returns field error")
        void missingEmail_shouldReturn422() throws Exception {
            var body = Map.of("password", "password1");

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Invalid field"))
                    .andExpect(jsonPath("$.errors.email").isArray());
        }

        @Test
        @DisplayName("422 — missing password returns field error")
        void missingPassword_shouldReturn422() throws Exception {
            var body = Map.of("email", "user1@webtech.id");

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Invalid field"))
                    .andExpect(jsonPath("$.errors.password").isArray());
        }

        @Test
        @DisplayName("422 — password shorter than 5 chars returns field error")
        void shortPassword_shouldReturn422() throws Exception {
            var body = Map.of("email", "user1@webtech.id", "password", "abc");

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors.password").isArray());
        }

        @Test
        @DisplayName("422 — both fields missing returns errors for email and password")
        void bothFieldsMissing_shouldReturn422WithBothErrors() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errors.email").isArray())
                    .andExpect(jsonPath("$.errors.password").isArray());
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/auth/logout
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class Logout {

        @Test
        @DisplayName("200 — valid Bearer token returns 'Logout success'")
        void validToken_shouldReturn200() throws Exception {
            mockMvc.perform(post(LOGOUT_URL)
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logout success"));
        }

        @Test
        @DisplayName("401 — missing Authorization header returns 'Unauthenticated.'")
        void noToken_shouldReturn401() throws Exception {
            mockMvc.perform(post(LOGOUT_URL))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Unauthenticated."));
        }

        @Test
        @DisplayName("401 — malformed token returns 'Unauthenticated.'")
        void malformedToken_shouldReturn401() throws Exception {
            mockMvc.perform(post(LOGOUT_URL)
                            .header("Authorization", "Bearer this.is.not.valid"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Unauthenticated."));
        }

        @Test
        @DisplayName("401 — missing 'Bearer ' prefix returns 'Unauthenticated.'")
        void wrongScheme_shouldReturn401() throws Exception {
            mockMvc.perform(post(LOGOUT_URL)
                            .header("Authorization", validToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Unauthenticated."));
        }
    }
}
