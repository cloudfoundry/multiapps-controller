package org.cloudfoundry.multiapps.controller.persistence.util;

public final class ConfigurationEntriesUtil {

    private ConfigurationEntriesUtil() {
    }

    private static final String PROVIDER_NAMESPACE_DEFAULT_VALUE = "default";

    public static boolean providerNamespaceIsEmpty(String providerNamespace, boolean considerNullAsEmpty) {
        return (considerNullAsEmpty && providerNamespace == null) || PROVIDER_NAMESPACE_DEFAULT_VALUE.equals(providerNamespace);
    }

}
