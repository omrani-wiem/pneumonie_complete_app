package com.example.pneumonia.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PasswordPolicyTest {
    @Test
    void rejectsWeakPasswords() {
        assertNotNull(AuthController.passwordError("court1!"));
        assertNotNull(AuthController.passwordError("1234567890!!"));   // sans lettre
        assertNotNull(AuthController.passwordError("abcdefghij!!"));   // sans chiffre
        assertNotNull(AuthController.passwordError("abcdefghij12"));   // sans caractère spécial
    }

    @Test
    void acceptsStrongPassword() {
        assertNull(AuthController.passwordError("Mot-de-passe-1!"));
    }
}