package org.cloudfoundry.multiapps.controller.core.helpers.v2;

import java.util.List;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.controller.core.helpers.common.AbstractConfigurationSubscriptionFactoryTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ConfigurationSubscriptionFactoryTest extends AbstractConfigurationSubscriptionFactoryTest {

    protected final Tester tester = Tester.forClass(getClass());

    @ParameterizedTest
    @MethodSource("testCreateSource")
    void testCreate(String mtadFilePath, List<String> configurationResources, String spaceId, Expectation expectation) {
        executeTestCreate(tester, mtadFilePath, configurationResources, spaceId, expectation);
    }

    public static Stream<Arguments> testCreateSource() {
        return Stream.of(
                         // (0) The required dependency is managed, so a subscription should be created:
                         Arguments.of("subscriptions-mtad-00.yaml", List.of("plugins"), "SPACE_ID_1",
                                      new Expectation(Expectation.Type.JSON, "subscriptions-00.json")),
                         // (1) The required dependency is not managed, so a subscription should not be created:
                         Arguments.of("subscriptions-mtad-01.yaml", List.of("plugins"), "SPACE_ID_1", new Expectation("[]")),
                         // (2) The required dependency is not managed, so a subscription should not be created:
                         Arguments.of("subscriptions-mtad-02.yaml", List.of("plugins"), "SPACE_ID_1", new Expectation("[]"))

        );
    }

}
