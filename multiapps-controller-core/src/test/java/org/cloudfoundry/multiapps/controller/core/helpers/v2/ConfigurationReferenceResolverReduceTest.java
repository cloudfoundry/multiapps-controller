package org.cloudfoundry.multiapps.controller.core.helpers.v2;

import java.util.List;

import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationReferenceResolverReduceTest {

    private static final String NID = "mta";
    private static final String PID = "com.sap.apm.assetbase:inspection-srv-api";
    private static final CloudTarget TARGET = new CloudTarget("initial", "initial");
    private static final CloudTarget OTHER_TARGET = new CloudTarget("other-org", "initial");

    private final ConfigurationReferenceResolver resolver = new ConfigurationReferenceResolver(null, null);

    private static ConfigurationEntry entry(String version, String namespace, CloudTarget target, String content) {
        return new ConfigurationEntry(NID, PID, version == null ? null : Version.parseVersion(version), namespace, target, content,
                                      List.of(), "space-guid", null);
    }

    private static ConfigurationEntry entry(String version, String content) {
        return entry(version, null, TARGET, content);
    }

    @Test
    void singleEntryIsReturnedUnchanged() {
        ConfigurationEntry only = entry("1.0.0-20261110120000", "{\"url\":\"a\"}");
        List<ConfigurationEntry> result = resolver.reduceRedundantEntries(List.of(only));
        assertEquals(1, result.size());
        assertSame(only, result.getFirst());
    }

    @Test
    void emptyListIsReturnedUnchanged() {
        assertTrue(resolver.reduceRedundantEntries(List.of())
                           .isEmpty());
    }

    @Test
    void twoVersionsSameProviderDifferentContentResolveToNewest() {
        ConfigurationEntry older = entry("1.0.0-20261110062256", "{\"url\":\"old\"}");
        ConfigurationEntry newer = entry("1.0.0-20261110083334", "{\"url\":\"new\"}");
        List<ConfigurationEntry> result = resolver.reduceRedundantEntries(List.of(older, newer));
        assertEquals(1, result.size());
        assertSame(newer, result.getFirst());
    }

    @Test
    void newestIsChosenRegardlessOfInputOrder() {
        ConfigurationEntry older = entry("1.0.0-20261110062256", "{\"url\":\"old\"}");
        ConfigurationEntry newer = entry("1.0.0-20261110083334", "{\"url\":\"new\"}");
        List<ConfigurationEntry> result = resolver.reduceRedundantEntries(List.of(newer, older));
        assertEquals(1, result.size());
        assertSame(newer, result.getFirst());
    }

    @Test
    void buildMetadataIsIgnoredWhenComparingVersions() {
        ConfigurationEntry older = entry("1.0.0-20261110074447+15f48c6a", "{\"url\":\"old\"}");
        ConfigurationEntry newer = entry("1.0.0-20261110083334+ab12cd3", "{\"url\":\"new\"}");
        List<ConfigurationEntry> result = resolver.reduceRedundantEntries(List.of(older, newer));
        assertEquals(1, result.size());
        assertSame(newer, result.getFirst());
    }

    @Test
    void identicalContentCollapsesEvenAcrossDifferentTargets() {
        ConfigurationEntry a = entry("2.0.0", null, TARGET, "{\"url\":\"same\"}");
        ConfigurationEntry b = entry("1.2.0", null, OTHER_TARGET, "{\"url\":\"same\"}");
        List<ConfigurationEntry> result = resolver.reduceRedundantEntries(List.of(a, b));
        assertEquals(1, result.size());
    }

    @Test
    void differentTargetsWithDifferentContentAreNotReduced() {
        ConfigurationEntry a = entry("2.0.0", null, TARGET, "{\"url\":\"a\"}");
        ConfigurationEntry b = entry("1.2.0", null, OTHER_TARGET, "{\"url\":\"b\"}");
        List<ConfigurationEntry> result = resolver.reduceRedundantEntries(List.of(a, b));
        assertEquals(2, result.size());
    }

    @Test
    void differentNamespacesWithDifferentContentAreNotReduced() {
        ConfigurationEntry a = entry("1.0.0-20261110083334", "ns-a", TARGET, "{\"url\":\"a\"}");
        ConfigurationEntry b = entry("1.0.0-20261110083334", "ns-b", TARGET, "{\"url\":\"b\"}");
        List<ConfigurationEntry> result = resolver.reduceRedundantEntries(List.of(a, b));
        assertEquals(2, result.size());
    }

    @Test
    void distinctProvidersEachKeepTheirNewestVersion() {
        ConfigurationEntry p1old = new ConfigurationEntry(NID, "mta-a:dep", Version.parseVersion("1.0.0"), null, TARGET,
                                                          "{\"url\":\"a-old\"}", List.of(), "s", null);
        ConfigurationEntry p1new = new ConfigurationEntry(NID, "mta-a:dep", Version.parseVersion("2.0.0"), null, TARGET,
                                                          "{\"url\":\"a-new\"}", List.of(), "s", null);
        ConfigurationEntry p2 = new ConfigurationEntry(NID, "mta-b:dep", Version.parseVersion("1.0.0"), null, TARGET,
                                                       "{\"url\":\"b\"}", List.of(), "s", null);
        List<ConfigurationEntry> result = resolver.reduceRedundantEntries(List.of(p1old, p1new, p2));
        assertEquals(2, result.size());
        assertTrue(result.contains(p1new));
        assertTrue(result.contains(p2));
    }

    @Test
    void threeVersionsOfSameProviderCollapseToNewest() {
        ConfigurationEntry v1 = entry("1.0.0-20261110060000", "{\"url\":\"1\"}");
        ConfigurationEntry v2 = entry("1.0.0-20261110070000", "{\"url\":\"2\"}");
        ConfigurationEntry v3 = entry("1.0.0-20261110080000", "{\"url\":\"3\"}");
        List<ConfigurationEntry> result = resolver.reduceRedundantEntries(List.of(v1, v3, v2));
        assertEquals(1, result.size());
        assertSame(v3, result.getFirst());
    }
}
