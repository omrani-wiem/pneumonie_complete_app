package com.example.pneumonia.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTest {

    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void blocksAfterMaxAttemptsThenReleasesWhenWindowEnds() {
        MutableClock clock = new MutableClock();
        LoginAttemptService s = new LoginAttemptService(3, Duration.ofMinutes(15), clock);
        for (int i = 0; i < 3; i++) {
            assertEquals(0, s.retryAfterSeconds("a@x", "1.1.1.1"));
            s.recordFailure("a@x", "1.1.1.1");
        }
        assertTrue(s.retryAfterSeconds("a@x", "1.1.1.1") > 0);
        clock.now = clock.now.plus(Duration.ofMinutes(16));
        assertEquals(0, s.retryAfterSeconds("a@x", "1.1.1.1"));
    }

    @Test
    void successResetsEmailCounter() {
        LoginAttemptService s = new LoginAttemptService(2, Duration.ofMinutes(15), new MutableClock());
        s.recordFailure("a@x", "1.1.1.1");
        s.recordSuccess("a@x");
        s.recordFailure("a@x", "2.2.2.2");
        assertEquals(0, s.retryAfterSeconds("a@x", "3.3.3.3"));
    }
}