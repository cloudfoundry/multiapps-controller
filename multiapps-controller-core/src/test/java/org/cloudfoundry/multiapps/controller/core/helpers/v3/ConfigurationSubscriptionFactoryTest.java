package org.cloudfoundry.multiapps.controller.core.helpers.v3;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.controller.core.model.ResolvedConfigurationReference;
import org.cloudfoundry.multiapps.mta.handlers.v3.DescriptorParser;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.junit.jupiter.params.provider.Arguments;

class ConfigurationSubscriptionFactoryTest
    extends org.cloudfoundry.multiapps.controller.core.helpers.v2.ConfigurationSubscriptionFactoryTest {

    public static Stream<Arguments> testCreate() {
        return Stream.of(
// @formatter:off
            // (0) The required dependency is managed, so a subscription should be created:
            Arguments.of("subscriptions-mtad-00.yaml", List.of("plugins"), "SPACE_ID_1", new Expectation(Expectation.Type.JSON, "subscriptions-00.json")),
            // (1) The required dependency is not managed, so a subscription should not be created:
            Arguments.of("subscriptions-mtad-01.yaml", List.of("plugins"), "SPACE_ID_1", new Expectation("[]")),
            // (2) The required dependency is not managed, so a subscription should not be created:
            Arguments.of("subscriptions-mtad-02.yaml", List.of("plugins"), "SPACE_ID_1", new Expectation("[]")),
            // (3) The required dependency is not active, so a subscription should not be created:
            Arguments.of("subscriptions-mtad-03.yaml", List.of("plugins"), "SPACE_ID_1", new Expectation("[]"))
// @formatter:on
        );
    }

    @Override
    protected void testCreate(DeploymentDescriptor mtad, Map<String, ResolvedConfigurationReference> resolvedResources, String spaceId,
                              Expectation expectation) {
        tester.test(() -> new ConfigurationSubscriptionFactory(mtad, resolvedResources, Collections.emptySet()).create(spaceId),
                    expectation);
    }

    @Override
    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

}
