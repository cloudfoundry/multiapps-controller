package com.sap.cloud.lm.sl.cf.core.filters;

import java.util.function.BiPredicate;

import org.cloudfoundry.multiapps.mta.model.Version;

import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;

public class VersionFilter implements BiPredicate<ConfigurationEntry, String> {

    @Override
    public boolean test(ConfigurationEntry entry, String requirement) {
        if (requirement == null) {
            return true;
        }
        Version providerVersion = entry.getProviderVersion();
        if (providerVersion == null) {
            return false;
        }
        return providerVersion.satisfies(requirement);
    }

}
