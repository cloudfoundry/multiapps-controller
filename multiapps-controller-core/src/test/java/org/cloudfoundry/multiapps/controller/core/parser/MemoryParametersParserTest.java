package org.cloudfoundry.multiapps.controller.core.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MemoryParametersParserTest {

    private static final Integer DEFAULT_MEMORY = 100;
    private final MemoryParametersParser parser = new MemoryParametersParser(SupportedParameters.MEMORY, "100");

    private static Stream<Arguments> testParameters() {
        return Stream.of(
// @formatter:off
            Arguments.of("2m", 2, null),
            Arguments.of("4M", 4, null),
            Arguments.of("5mb", 5, null),
            Arguments.of("10MB", 10, null),
            Arguments.of("5mB", 5, null),
            Arguments.of("2g", 2 * 1024, null),
            Arguments.of("3gb", 3 * 1024, null),
            Arguments.of("5G", 5 * 1024, null),
            Arguments.of("6GB", 6 * 1024, null),
            Arguments.of("12gB", 12 * 1024, null),
            Arguments.of("100", 100, null)
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource("testParameters")
    void testMemoryParsing(String memoryStr, Integer expectedParsedMemory) {
        List<Map<String, Object>> parametersList = Collections.singletonList(Map.of(SupportedParameters.MEMORY, memoryStr));

        Integer memory = parser.parse(parametersList);

        assertEquals(expectedParsedMemory, memory);
    }

    @Test
    void testInvalidMemoryParsing() {
        List<Map<String, Object>> parametersList = Collections.singletonList(Map.of(SupportedParameters.MEMORY, "test-mb"));

        assertThrows(ContentException.class, () -> parser.parse(parametersList));
    }

    @Test
    void testDefaultMemoryParsing() {
        Integer memory = parser.parse(Collections.emptyList());

        assertEquals(DEFAULT_MEMORY, memory);
    }
}
