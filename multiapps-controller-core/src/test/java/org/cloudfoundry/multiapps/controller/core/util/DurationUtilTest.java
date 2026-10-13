package org.cloudfoundry.multiapps.controller.core.util;

import java.time.Duration;
import java.util.Optional;

import org.cloudfoundry.multiapps.common.ContentException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurationUtilTest {

    private static final String PARAM_NAME = "test-timeout";
    private static final int MAX_VALUE = 3600;

    @Test
    void testParseDurationWithValidValue() {
        Duration result = DurationUtil.parseDuration(300, PARAM_NAME, MAX_VALUE);
        assertEquals(Duration.ofSeconds(300), result);
    }

    @Test
    void testParseDurationWithZero() {
        Duration result = DurationUtil.parseDuration(0, PARAM_NAME, MAX_VALUE);
        assertEquals(Duration.ZERO, result);
    }

    @Test
    void testParseDurationWithMaxValue() {
        Duration result = DurationUtil.parseDuration(MAX_VALUE, PARAM_NAME, MAX_VALUE);
        assertEquals(Duration.ofSeconds(MAX_VALUE), result);
    }

    @Test
    void testParseDurationWithNull() {
        Duration result = DurationUtil.parseDuration(null, PARAM_NAME, MAX_VALUE);
        assertNull(result);
    }

    @Test
    void testParseDurationWithNegativeValue() {
        assertThrows(ContentException.class, () -> DurationUtil.parseDuration(-1, PARAM_NAME, MAX_VALUE));
    }

    @Test
    void testParseDurationWithValueExceedingMax() {
        assertThrows(ContentException.class, () -> DurationUtil.parseDuration(MAX_VALUE + 1, PARAM_NAME, MAX_VALUE));
    }

    @Test
    void testParseDurationWithNonNumber() {
        assertThrows(ContentException.class, () -> DurationUtil.parseDuration("invalid", PARAM_NAME, MAX_VALUE));
    }

    @Test
    void testParseDurationSafelyWithValidValue() {
        Optional<Duration> result = DurationUtil.parseDurationSafely(300);
        assertTrue(result.isPresent());
        assertEquals(Duration.ofSeconds(300), result.get());
    }

    @Test
    void testParseDurationSafelyWithNull() {
        Optional<Duration> result = DurationUtil.parseDurationSafely(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testParseDurationSafelyWithZero() {
        Optional<Duration> result = DurationUtil.parseDurationSafely(0);
        assertTrue(result.isEmpty());
    }

    @Test
    void testParseDurationSafelyWithNegative() {
        Optional<Duration> result = DurationUtil.parseDurationSafely(-5);
        assertTrue(result.isEmpty());
    }

    @Test
    void testParseDurationSafelyWithNonNumber() {
        Optional<Duration> result = DurationUtil.parseDurationSafely("invalid");
        assertTrue(result.isEmpty());
    }
}

