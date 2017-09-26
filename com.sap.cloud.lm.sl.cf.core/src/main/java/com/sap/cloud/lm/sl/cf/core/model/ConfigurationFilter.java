package com.sap.cloud.lm.sl.cf.core.model;

import java.util.Map;
import java.util.function.BiFunction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.common.model.json.PropertiesAdapterFactory;

public class ConfigurationFilter {

    @Expose
    private String providerId;
    @Expose
    private Map<String, Object> requiredContent;
    @Expose
    private String providerNid;
    @Expose
    @SerializedName("targetSpace")
    private CloudTarget targetSpace;
    @Expose
    private String providerVersion;

    private transient boolean strictTargetSpace;

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
        if (providerVersion != null && (entry.getProviderVersion() == null || !entry.getProviderVersion().satisfies(providerVersion))) {
            return false;
        }
        if (requiredContent != null && !CONTENT_FILTER.apply(entry.getContent(), requiredContent)) {
            return false;
        }
        return true;
    }

    public static final BiFunction<String, Map<String, Object>, Boolean> CONTENT_FILTER = new BiFunction<String, Map<String, Object>, Boolean>() {

        @Override
        public Boolean apply(String content, Map<String, Object> requiredProperties) {
            if (requiredProperties == null || requiredProperties.isEmpty()) {
                return true;
            }
            Map<String, Object> parsedContent = getParsedContent(content);
            if (parsedContent == null) {
                return false;
            }
            return requiredProperties.entrySet().stream().allMatch((requiredEntry) -> exists(parsedContent, requiredEntry));
        }

        private boolean exists(Map<String, Object> content, Map.Entry<String, Object> requiredEntry) {
            Object actualValue = content.get(requiredEntry.getKey());
            return actualValue != null && actualValue.equals(requiredEntry.getValue());
        }

        private Map<String, Object> getParsedContent(String content) {
            if (content == null) {
                return null;
            }
            try {
                Gson gson = new GsonBuilder().registerTypeAdapterFactory(new PropertiesAdapterFactory()).create();
                return gson.fromJson(content, new TypeToken<Map<String, Object>>() {
                }.getType());
            } catch (Exception e) {
                return null;
            }
        }

    };

}
