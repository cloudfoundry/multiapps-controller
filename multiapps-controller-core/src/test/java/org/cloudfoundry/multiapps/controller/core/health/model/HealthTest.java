package org.cloudfoundry.multiapps.controller.core.health.model;

import java.util.List;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.type.TypeReference;

class HealthTest {

    private final Tester tester = Tester.forClass(getClass());

    public static Stream<Arguments> testFromOperations() {
        return Stream.of(
        // @formatter:off
            // (0)
            Arguments.of("successful-operations.json", new Expectation(Expectation.Type.JSON, "good-health.json")),
            // (1)
            Arguments.of("failed-operations.json", new Expectation(Expectation.Type.JSON, "poor-health.json"))
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFromOperations(String operationsJsonLocation, Expectation expectation) {
        String operationsJson = TestUtil.getResourceAsString(operationsJsonLocation, getClass());
        List<Operation> operations = JsonUtil.fromJson(operationsJson, new TypeReference<>() {
        });
        tester.test(() -> Health.fromOperations(operations), expectation);
    }

}
