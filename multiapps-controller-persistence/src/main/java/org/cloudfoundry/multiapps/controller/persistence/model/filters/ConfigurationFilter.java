package org.cloudfoundry.multiapps.controller.persistence.model.filters;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.persistence.util.ConfigurationEntriesUtil;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ConfigurationFilter {

    private String providerId;
    private Map<String, Object> requiredContent;
    private String providerNid;
    private CloudTarget targetSpace;
    private String providerVersion;
    private String providerNamespace;
    @JsonIgnore
    private boolean strictTargetSpace;

    public ConfigurationFilter() {
        // Required by Jackson
    }

    public ConfigurationFilter(String providerNid, String providerId, String providerVersion, String providerNamespace,
                               CloudTarget targetSpace, Map<String, Object> requiredContent) {
        this(providerNid, providerId, providerVersion, providerNamespace, targetSpace, requiredContent, true);
    }

    public ConfigurationFilter(String providerNid, String providerId, String providerVersion, String providerNamespace,
                               CloudTarget targetSpace, Map<String, Object> requiredContent, boolean strictTargetSpace) {
        this.providerNid = providerNid;
        this.providerId = providerId;
        this.providerVersion = providerVersion;
        this.providerNamespace = providerNamespace;
        this.targetSpace = targetSpace;
        this.requiredContent = requiredContent;
        this.strictTargetSpace = strictTargetSpace;
    }

    public String getProviderVersion() {
        return providerVersion;
    }

    public Map<String, Object> getRequiredContent() {
        return requiredContent;
    }

    public String getProviderNid() {
        return providerNid;
    }

    public CloudTarget getTargetSpace() {
        return targetSpace;
    }

    public String getProviderId() {
        return providerId;
    }

    public boolean isStrictTargetSpace() {
        return strictTargetSpace;
    }

    public String getProviderNamespace() {
        return providerNamespace;
    }

    public boolean matches(ConfigurationEntry entry) {
        if (providerNid != null && !providerNid.equals(entry.getProviderNid())) {
            return false;
        }
        if (targetSpace != null && !targetSpace.equals(entry.getTargetSpace())) {
            return false;
        }
        if (providerId != null && !providerId.equals(entry.getProviderId())) {
            return false;
        }
        if (providerVersion != null && (entry.getProviderVersion() == null || !entry.getProviderVersion()
                                                                                    .satisfies(providerVersion))) {
            return false;
        }
        if (!namespaceConstraintIsSatisfied(entry.getProviderNamespace())) {
            return false;
        }
        return new ContentFilter().test(entry.getContent(), requiredContent);
    }

    private boolean namespaceConstraintIsSatisfied(String providerNamespace) {
        if (this.providerNamespace == null) {
            return true;
        }

        if (ConfigurationEntriesUtil.providerNamespaceIsEmpty(this.providerNamespace, true) && providerNamespace == null) {
            return true;
        }

        return this.providerNamespace.equals(providerNamespace);
    }

}
