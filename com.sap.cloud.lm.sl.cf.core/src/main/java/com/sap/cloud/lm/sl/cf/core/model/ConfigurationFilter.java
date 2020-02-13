package com.sap.cloud.lm.sl.cf.core.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sap.cloud.lm.sl.cf.core.filters.ContentFilter;

public class ConfigurationFilter {

    private String providerId;
    private Map<String, Object> requiredContent;
    private String providerNid;
    private CloudTarget targetSpace;
    private String providerVersion;
    @JsonIgnore
    private boolean strictTargetSpace;

    public ConfigurationFilter() {
        // Required by Jackson
    }

    public ConfigurationFilter(String providerNid, String providerId, String providerVersion, CloudTarget targetSpace,
                               Map<String, Object> requiredContent) {
        this(providerNid, providerId, providerVersion, targetSpace, requiredContent, true);
    }

    public ConfigurationFilter(String providerNid, String providerId, String providerVersion, CloudTarget targetSpace,
                               Map<String, Object> requiredContent, boolean strictTargetSpace) {
        this.providerId = providerId;
        this.requiredContent = requiredContent;
        this.providerNid = providerNid;
        this.targetSpace = targetSpace;
        this.providerVersion = providerVersion;
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
        return new ContentFilter().test(entry.getContent(), requiredContent);
    }

}
