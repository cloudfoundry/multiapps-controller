package org.cloudfoundry.multiapps.controller.core.helpers.v2;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.controller.core.helpers.common.AbstractConfigurationReferencesResolverTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ConfigurationReferencesResolverTest extends AbstractConfigurationReferencesResolverTest {

    private final Tester tester = Tester.forClass(getClass());

    @ParameterizedTest
    @MethodSource("testResolveSource")
    void testResolve(String descriptorLocation, String configurationEntriesLocation, Expectation expectation) {
        executeTestResolve(tester, descriptorLocation, configurationEntriesLocation, expectation);
    }

    static Stream<Arguments> testResolveSource() {
        return Stream.of(
                // (1) Reference to existing provided dependency:
                Arguments.of("mtad-03.yaml", "configuration-entries-01.json",
                        new Expectation(Expectation.Type.JSON, "result-01.json")),
                // (2) Use new syntax:
                Arguments.of("mtad-05.yaml", "configuration-entries-01.json",
                        new Expectation(Expectation.Type.JSON, "result-01.json")),
                // (3) Use new syntax when more than one configuration entries are available:
                Arguments.of("mtad-05.yaml", "configuration-entries-05.json",
                        new Expectation(Expectation.Type.EXCEPTION,
                                "Multiple configuration entries were found matching the filter specified in resource \"resource-2\"")),
                // (4) Use new syntax when more than one configuration entries are available:
                Arguments.of("mtad-07.yaml", "configuration-entries-06.json",
                        new Expectation(Expectation.Type.JSON, "result-02.json")),
                // (5) Use new syntax when there is no configuration entry available:
                Arguments.of("mtad-05.yaml", "configuration-entries-04.json",
                        new Expectation(Expectation.Type.EXCEPTION,
                                "No configuration entries were found matching the filter specified in resource \"resource-2\"")),
                // (6) Use new syntax when there is no configuration entry available:
                Arguments.of("mtad-07.yaml", "configuration-entries-07.json",
                        new Expectation(Expectation.Type.JSON, "result-03.json")),
                // (7) Use new syntax (missing org parameter):
                Arguments.of("mtad-06.yaml", "configuration-entries-01.json",
                        new Expectation(Expectation.Type.EXCEPTION, "Could not find required property \"org\"")),
                // (8) Subscriptions should be created:
                Arguments.of("mtad-08.yaml", "configuration-entries-06.json",
                        new Expectation(Expectation.Type.JSON, "result-04.json")));
    }

}
