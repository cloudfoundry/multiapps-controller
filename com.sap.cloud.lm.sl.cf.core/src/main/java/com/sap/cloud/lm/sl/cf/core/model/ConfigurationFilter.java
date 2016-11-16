package com.sap.cloud.lm.sl.cf.core.model;

import java.util.Map;
import java.util.function.BiFunction;

import com.google.gson.annotations.Expose;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class ConfigurationFilter {

    @Expose
    private String providerId;
    @Expose
    private Map<String, String> requiredContent;
    @Expose
    private String providerNid;
    @Expose
    private String targetSpace;
    @Expose
    private String providerVersion;

    public ConfigurationFilter(String providerNid, String providerId, String providerVersion, String targetSpace,
        Map<String, String> requiredContent) {
        this.providerId = providerId;
        this.requiredContent = requiredContent;
        this.providerNid = providerNid;
        this.targetSpace = targetSpace;
        this.providerVersion = providerVersion;
    }

    public String getProviderVersion() {
        return providerVersion;
    }

    public Map<String, String> getRequiredContent() {
        return requiredContent;
    }

    public String getProviderNid() {
        return providerNid;
    }

    public String getTargetSpace() {
        return targetSpace;
    }

    public String getProviderId() {
        return providerId;
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
        if (providerVersion != null && (entry.getProviderVersion() == null || !entry.getProviderVersion().satisfies(providerVersion))) {
            return false;
        }
        if (requiredContent != null && !CONTENT_FILTER.apply(entry.getContent(), requiredContent)) {
            return false;
        }
        return true;
    }

    public static final BiFunction<String, Map<String, String>, Boolean> CONTENT_FILTER = new BiFunction<String, Map<String, String>, Boolean>() {

        @Override
        public Boolean apply(String content, Map<String, String> requiredProperties) {
            if (requiredProperties == null || requiredProperties.isEmpty()) {
                return true;
            }
            Map<String, Object> parsedContent = getParsedContent(content);
            if (parsedContent == null) {
                return false;
            }
            return requiredProperties.entrySet().stream().allMatch((requiredEntry) -> exists(parsedContent, requiredEntry));
        }

        private boolean exists(Map<String, Object> content, Map.Entry<String, String> requiredEntry) {
            Object actualValue = content.get(requiredEntry.getKey());
            return actualValue != null && actualValue.equals(requiredEntry.getValue());
        }

        private Map<String, Object> getParsedContent(String content) {
            if (content == null) {
                return null;
            }
            try {
                return JsonUtil.convertJsonToMap(content);
            } catch (ParsingException e) {
                return null;
            }
        }

    };

}
