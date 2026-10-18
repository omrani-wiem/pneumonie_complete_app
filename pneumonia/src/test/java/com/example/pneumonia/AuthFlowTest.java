package com.example.pneumonia;

import com.example.pneumonia.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowTest extends IntegrationTestBase {

    private String json(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    @Test
    void weakPasswordIsRejectedAtRegistration() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json("a@test.local", "abcdefgh"))).andExpect(status().isBadRequest());
        assertFalse(users.existsByEmail("a@test.local"));
    }

    @Test
    void registeredAccountIsDisabledUntilValidated() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json("new@test.local", "Mot-de-passe-1!"))).andExpect(status().isCreated());
        assertFalse(users.findByEmail("new@test.local").orElseThrow().isEnabled());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json("new@test.local", "Mot-de-passe-1!"))).andExpect(status().isForbidden());
    }

    @Test
    void duplicateEmailIsRefused() throws Exception {
        user("dup@test.local", Role.USER, true);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json("dup@test.local", "Mot-de-passe-1!"))).andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        user("ok@test.local", Role.USER, true);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json("ok@test.local", "mauvais"))).andExpect(status().isUnauthorized());
    }

    @Test
    void successfulLoginSetsHttpOnlyCookieAndNoTokenInBody() throws Exception {
        user("ok@test.local", Role.USER, true);
        var res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json("ok@test.local", "Mot-de-passe-1!"))).andExpect(status().isOk()).andReturn()
                .getResponse();
        String cookie = res.getHeader("Set-Cookie");
        assertNotNull(cookie);
        assertTrue(cookie.contains("access_token="));
        assertTrue(cookie.contains("HttpOnly"));
        assertFalse(res.getContentAsString().contains("token"));
    }
}