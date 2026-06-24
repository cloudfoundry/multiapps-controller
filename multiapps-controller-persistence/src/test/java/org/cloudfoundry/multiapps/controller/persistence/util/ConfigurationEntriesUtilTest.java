package org.cloudfoundry.multiapps.controller.persistence.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ConfigurationEntriesUtilTest {

    @Test
    void testDefaultProviderNamespaceIsEmpty() {
        Assertions.assertTrue(ConfigurationEntriesUtil.providerNamespaceIsEmpty("default", true));
        Assertions.assertTrue(ConfigurationEntriesUtil.providerNamespaceIsEmpty("default", false));
    }

    @Test
    void testNullIsEmptyOnlyWhenConsiderNullAsEmpty() {
        Assertions.assertTrue(ConfigurationEntriesUtil.providerNamespaceIsEmpty(null, true));
        Assertions.assertFalse(ConfigurationEntriesUtil.providerNamespaceIsEmpty(null, false));
    }

    @Test
    void testNonDefaultNonNullIsNotEmpty() {
        Assertions.assertFalse(ConfigurationEntriesUtil.providerNamespaceIsEmpty("custom", true));
        Assertions.assertFalse(ConfigurationEntriesUtil.providerNamespaceIsEmpty("custom", false));
    }

    @Test
    void testEmptyStringIsNotConsideredEmpty() {
        Assertions.assertFalse(ConfigurationEntriesUtil.providerNamespaceIsEmpty("", true));
        Assertions.assertFalse(ConfigurationEntriesUtil.providerNamespaceIsEmpty("", false));
    }
}
