package com.sap.cloud.lm.sl.cf.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.TestUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.type.TypeReference;

class ConfigurationFilterTest {

    static Stream<Arguments> testMatches() {
        return Stream.of(
        // @formatter:off
        // (1) Filter checks for empty namespace with keyword 'default' and matches
        Arguments.of("configuration-filter-test-input-01.json"),
        // (2) Filter doesn't contain namespace and matches with any namespace
        Arguments.of("configuration-filter-test-input-02.json"),
        // (3) Filter checks for specific namespace and matches
        Arguments.of("configuration-filter-test-input-03.json"),
        // (4) Filter checks for default/empty namespace and doesn't match
        Arguments.of("configuration-filter-test-input-04.json"),
        // (5) Filter checks for specific namespace but it doesn't match
        Arguments.of("configuration-filter-test-input-05.json"),
        // (6) Filter checks for specific namespace but entry doesn't have one
        Arguments.of("configuration-filter-test-input-06.json"),
        // (7) Filter checks for higher version and doesn't match
        Arguments.of("configuration-filter-test-input-07.json")
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testMatches(String testInputLocation) {
        ConfigurationFilterTestInput input = parseInput(testInputLocation);

        assertEquals(input.filterResult, input.configurationFilter.matches(input.configurationEntry));
    }

    private ConfigurationFilterTestInput parseInput(String inputFileLocation) {
        if (inputFileLocation == null) {
            fail("Test requires a configuration entry");
        }

        String testInputJson = TestUtil.getResourceAsString(inputFileLocation, getClass());
        ConfigurationFilterTestInput testInput = JsonUtil.fromJson(testInputJson, new TypeReference<ConfigurationFilterTestInput>() {
        });

        return testInput;
    }

    private static class ConfigurationFilterTestInput {
        private ConfigurationEntry configurationEntry;
        private ConfigurationFilter configurationFilter;
        private boolean filterResult;

    }
}
