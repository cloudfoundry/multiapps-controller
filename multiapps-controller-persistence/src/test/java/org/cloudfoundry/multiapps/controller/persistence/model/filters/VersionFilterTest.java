package org.cloudfoundry.multiapps.controller.persistence.model.filters;

import java.util.Collections;

import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class VersionFilterTest {

    private final VersionFilter filter = new VersionFilter();

    @Test
    void testNullRequirementMatchesAnything() {
        ConfigurationEntry entry = newEntry(Version.parseVersion("1.0.0"));

        Assertions.assertTrue(filter.test(entry, null));
    }

    @Test
    void testNullProviderVersionFailsAnyConstraint() {
        ConfigurationEntry entry = newEntry(null);

        Assertions.assertFalse(filter.test(entry, "1.0.0"));
    }

    @Test
    void testProviderVersionMatchesRequirement() {
        ConfigurationEntry entry = newEntry(Version.parseVersion("1.2.3"));

        Assertions.assertTrue(filter.test(entry, "1.2.3"));
    }

    @Test
    void testProviderVersionFailsRequirement() {
        ConfigurationEntry entry = newEntry(Version.parseVersion("1.0.0"));

        Assertions.assertFalse(filter.test(entry, "2.0.0"));
    }

    private ConfigurationEntry newEntry(Version providerVersion) {
        return new ConfigurationEntry(0, "nid", "id", providerVersion, null, null, null, Collections.emptyList(), null, null);
    }
}
