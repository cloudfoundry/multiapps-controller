package org.cloudfoundry.multiapps.controller.persistence.model.filters;

import java.util.Collections;
import java.util.List;

import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class VisibilityFilterTest {

    private final VisibilityFilter filter = new VisibilityFilter();

    @Test
    void testEmptyCloudTargetsMatchesAnything() {
        ConfigurationEntry entry = newEntry(null, new CloudTarget("my-org", "my-space"));

        Assertions.assertTrue(filter.test(entry, Collections.emptyList()));
    }

    @Test
    void testExactVisibilityMatch() {
        CloudTarget target = new CloudTarget("my-org", "my-space");
        ConfigurationEntry entry = newEntry(List.of(target), new CloudTarget("provider-org", "provider-space"));

        Assertions.assertTrue(filter.test(entry, List.of(target)));
    }

    @Test
    void testFullWildcardVisibilityMatchesEverything() {
        ConfigurationEntry entry = newEntry(List.of(new CloudTarget("*", "*")), new CloudTarget("provider-org", "provider-space"));

        Assertions.assertTrue(filter.test(entry, List.of(new CloudTarget("any-org", "any-space"))));
    }

    @Test
    void testOrgWildcardMatchesSameSpace() {
        ConfigurationEntry entry = newEntry(List.of(new CloudTarget("*", "my-space")), new CloudTarget("provider-org", "provider-space"));

        Assertions.assertTrue(filter.test(entry, List.of(new CloudTarget("any-org", "my-space"))));
    }

    @Test
    void testSpaceWildcardMatchesSameOrg() {
        ConfigurationEntry entry = newEntry(List.of(new CloudTarget("my-org", "*")), new CloudTarget("provider-org", "provider-space"));

        Assertions.assertTrue(filter.test(entry, List.of(new CloudTarget("my-org", "any-space"))));
    }

    @Test
    void testNullVisibilityFallsBackToProviderOrgWithAnySpace() {
        // When visibility is null, fall back to provider's org with "*" space.
        ConfigurationEntry entry = newEntry(null, new CloudTarget("provider-org", "provider-space"));

        Assertions.assertTrue(filter.test(entry, List.of(new CloudTarget("provider-org", "any-space"))));
        Assertions.assertFalse(filter.test(entry, List.of(new CloudTarget("other-org", "any-space"))));
    }

    @Test
    void testNoMatchReturnsFalse() {
        ConfigurationEntry entry = newEntry(List.of(new CloudTarget("visible-org", "visible-space")),
                                            new CloudTarget("provider-org", "provider-space"));

        Assertions.assertFalse(filter.test(entry, List.of(new CloudTarget("other-org", "other-space"))));
    }

    private ConfigurationEntry newEntry(List<CloudTarget> visibility, CloudTarget targetSpace) {
        return new ConfigurationEntry(0, "nid", "id", null, null, targetSpace, null, visibility, null, null);
    }
}
