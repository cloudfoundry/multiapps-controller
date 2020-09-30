package org.cloudfoundry.multiapps.controller.persistence.model.filters;

import java.util.function.BiPredicate;

import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.mta.model.Version;

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
