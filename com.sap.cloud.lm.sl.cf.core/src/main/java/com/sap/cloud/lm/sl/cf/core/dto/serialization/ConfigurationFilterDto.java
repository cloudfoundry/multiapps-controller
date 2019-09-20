package com.sap.cloud.lm.sl.cf.core.dto.serialization;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "configuration-filter")
public class ConfigurationFilterDto {

    @XmlElement(name = "provider-id")
    private String providerId;

    @XmlElement(name = "content")
    private List<String> content;

    @XmlElement(name = "provider-nid")
    private String providerNid;

    @XmlElement(name = "target-space")
    private String targetSpace;

    @XmlElement(name = "provider-target")
    private CloudTarget cloudTarget;

    @XmlElement(name = "provider-version")
    private String providerVersion;

    public ConfigurationFilterDto() {
        // Required by JAX-RS.
    }

    public ConfigurationFilterDto(String providerNid, String providerId, String version, CloudTarget cloudTarget, String targetSpace, List<String> content) {
        this.providerId = providerId;
        this.providerVersion = version;
        this.content = content;
        this.providerNid = providerNid;
        this.targetSpace = targetSpace;
        this.cloudTarget = cloudTarget;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getProviderVersion() {
        return providerVersion;
    }

    public String getProviderNid() {
        return providerNid;
    }

    public String getTargetSpace() {
        return targetSpace;
    }

    public List<String> getContent() {
        return content;
    }

    public CloudTarget getCloudTarget() {
        return cloudTarget;
    }
}
