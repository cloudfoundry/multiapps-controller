package org.cloudfoundry.multiapps.controller.core.util;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LogSanitizerTest {

    static Stream<Arguments> testSanitize() {
        return Stream.of(
            Arguments.of(null, null),
            Arguments.of("", ""),
            Arguments.of("plain text", "plain text"),
            Arguments.of("with spaces and punctuation!", "with spaces and punctuation!"),
            Arguments.of("a\nb", "a\\nb"),
            Arguments.of("a\rb", "a\\rb"),
            Arguments.of("a\tb", "a\\tb"),
            Arguments.of("a\r\nb", "a\\r\\nb"),
            Arguments.of("\tcol1\tcol2", "\\tcol1\\tcol2"),
            Arguments.of("line1\nline2\r\nline3\tend", "line1\\nline2\\r\\nline3\\tend"),
            Arguments.of("evil\nFAKE LOG ENTRY: forged", "evil\\nFAKE LOG ENTRY: forged"),
            Arguments.of("already escaped: \\n", "already escaped: \\n"));
    }

    @ParameterizedTest
    @MethodSource
    void testSanitize(String input, String expected) {
        assertEquals(expected, LogSanitizer.sanitize(input));
    }

    @Test
    void testSanitizeIsIdempotent() {
        String once = LogSanitizer.sanitize("a\nb\tc");
        String twice = LogSanitizer.sanitize(once);
        assertEquals(once, twice);
    }

    @Test
    void testSanitizeObjectNull() {
        assertNull(LogSanitizer.sanitize((Object) null));
    }

    @Test
    void testSanitizeObjectDelegatesToToString() {
        Object value = new Object() {
            @Override
            public String toString() {
                return "value-with\nnewline";
            }
        };
        assertEquals("value-with\\nnewline", LogSanitizer.sanitize(value));
    }
}
