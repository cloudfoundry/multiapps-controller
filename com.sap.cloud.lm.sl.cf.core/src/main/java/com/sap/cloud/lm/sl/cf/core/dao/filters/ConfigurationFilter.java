package com.sap.cloud.lm.sl.cf.core.dao.filters;

import java.util.Map;
import java.util.function.BiPredicate;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.common.model.xml.PropertiesAdapter;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@XmlRootElement(name = "configuration-filter")
@XmlAccessorType(value = XmlAccessType.FIELD)
public class ConfigurationFilter {

    @XmlElement(name = "provider-id")
    private String providerId;
    @XmlElement(name = "required-content")
    @XmlJavaTypeAdapter(value = PropertiesAdapter.class)
    private Map<String, Object> requiredContent;
    @XmlElement(name = "provider-nid")
    private String providerNid;
    @XmlElement(name = "target-space")
    private CloudTarget targetSpace;
    @XmlElement(name = "provider-version")
    private String providerVersion;
    @XmlTransient
    @JsonIgnore
    private boolean strictTargetSpace;

    public ConfigurationFilter() {

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
        return requiredContent == null || CONTENT_FILTER.test(entry.getContent(), requiredContent);
    }

    public static final BiPredicate<String, Map<String, Object>> CONTENT_FILTER = new BiPredicate<String, Map<String, Object>>() {

        @Override
        public boolean test(String content, Map<String, Object> requiredProperties) {
            if (requiredProperties == null || requiredProperties.isEmpty()) {
                return true;
            }
            Map<String, Object> parsedContent = getParsedContent(content);
            if (parsedContent == null) {
                return false;
            }
            return requiredProperties.entrySet()
                                     .stream()
                                     .allMatch(requiredEntry -> exists(parsedContent, requiredEntry));
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
                return JsonUtil.convertJsonToMap(content);
            } catch (Exception e) {
                return null;
            }
        }

    };

}
