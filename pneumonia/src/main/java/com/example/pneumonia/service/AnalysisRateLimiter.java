package com.example.pneumonia.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/** Fenêtre glissante d'une minute par utilisateur. En mémoire : valable pour une instance unique. */
@Component
public class AnalysisRateLimiter {

    private final ConcurrentHashMap<String, Deque<Instant>> hits = new ConcurrentHashMap<>();
    private final int maxPerMinute;

    public AnalysisRateLimiter(@Value("${app.analysis.max-per-minute:10}") int maxPerMinute) {
        this.maxPerMinute = maxPerMinute;
    }

    public boolean tryAcquire(String key) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(Duration.ofMinutes(1));
        Deque<Instant> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst().isBefore(cutoff)) q.pollFirst();
            if (q.size() >= maxPerMinute) return false;
            q.addLast(now);
            return true;
        }
    }
}