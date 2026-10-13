package com.example.pneumonia.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite les tentatives de connexion échouées, par adresse e-mail ET par adresse IP.
 * En mémoire : suffisant pour une instance unique.
 */
@Service
public class LoginAttemptService {

    private record Entry(int failures, Instant windowStart) {}

    private static final int MAX_ENTRIES = 10_000;

    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration window;
    private final Clock clock;

    @Autowired
    public LoginAttemptService(@Value("${app.login.max-attempts:5}") int maxAttempts,
                               @Value("${app.login.lockout-minutes:15}") long lockoutMinutes) {
        this(maxAttempts, Duration.ofMinutes(lockoutMinutes), Clock.systemUTC());
    }

    // Constructeur pour les tests (horloge contrôlable)
    LoginAttemptService(int maxAttempts, Duration window, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.clock = clock;
    }

    /** Secondes à attendre avant de pouvoir réessayer, ou 0 si la tentative est autorisée. */
    public long retryAfterSeconds(String email, String ip) {
        return Math.max(remaining("e:" + email), remaining("i:" + ip));
    }

    public void recordFailure(String email, String ip) {
        evictIfTooBig();
        bump("e:" + email);
        bump("i:" + ip);
    }

    /** Une connexion réussie remet à zéro le compteur de l'e-mail (pas celui de l'IP). */
    public void recordSuccess(String email) {
        entries.remove("e:" + email);
    }

    private long remaining(String key) {
        Entry e = entries.get(key);
        if (e == null) return 0;
        Instant now = clock.instant();
        Instant end = e.windowStart().plus(window);
        if (!now.isBefore(end)) {
            entries.remove(key, e);
            return 0;
        }
        return e.failures() >= maxAttempts ? Math.max(1, Duration.between(now, end).getSeconds()) : 0;
    }

    private void bump(String key) {
        Instant now = clock.instant();
        entries.merge(key, new Entry(1, now), (old, ignored) ->
                now.isBefore(old.windowStart().plus(window))
                        ? new Entry(old.failures() + 1, old.windowStart())
                        : new Entry(1, now));
    }

    // Évite que la table grossisse sans limite : on purge les entrées expirées
    private void evictIfTooBig() {
        if (entries.size() < MAX_ENTRIES) return;
        Instant now = clock.instant();
        entries.entrySet().removeIf(en -> !now.isBefore(en.getValue().windowStart().plus(window)));
    }
}