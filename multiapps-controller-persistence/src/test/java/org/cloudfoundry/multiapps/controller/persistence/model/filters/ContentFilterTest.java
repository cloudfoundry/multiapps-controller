package org.cloudfoundry.multiapps.controller.persistence.model.filters;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ContentFilterTest {

    private final ContentFilter filter = new ContentFilter();

    @Test
    void testEmptyRequiredPropertiesMatchesAnything() {
        Assertions.assertTrue(filter.test(null, Collections.emptyMap()));
        Assertions.assertTrue(filter.test("{\"a\":1}", Collections.emptyMap()));
    }

    @Test
    void testNullContentDoesNotMatchNonEmptyRequirements() {
        Assertions.assertFalse(filter.test(null, Map.of("a", 1)));
    }

    @Test
    void testInvalidJsonContentDoesNotMatch() {
        Assertions.assertFalse(filter.test("not-json", Map.of("a", 1)));
    }

    @Test
    void testAllRequiredPropertiesMatch() {
        String content = "{\"a\":1,\"b\":\"two\"}";

        Assertions.assertTrue(filter.test(content, Map.of("a", 1, "b", "two")));
    }

    @Test
    void testMissingPropertyFailsMatch() {
        String content = "{\"a\":1}";

        Assertions.assertFalse(filter.test(content, Map.of("a", 1, "b", "two")));
    }

    @Test
    void testWrongValueFailsMatch() {
        String content = "{\"a\":1}";

        Assertions.assertFalse(filter.test(content, Map.of("a", 2)));
    }
}
