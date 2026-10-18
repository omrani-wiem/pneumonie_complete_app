package com.example.pneumonia.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieService {

    public static final String NAME = "access_token";

    private final boolean secure;
    private final String sameSite;
    private final long maxAgeMinutes;

    public AuthCookieService(@Value("${app.cookie.secure:false}") boolean secure,
                             @Value("${app.cookie.same-site:Lax}") String sameSite,
                             @Value("${app.jwt.expiration-minutes}") long maxAgeMinutes) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.maxAgeMinutes = maxAgeMinutes;
    }

    public ResponseCookie build(String token) {
        return base(token).maxAge(Duration.ofMinutes(maxAgeMinutes)).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/");
    }
}