package com.sap.cloud.lm.sl.cf.core.dto.serialization;

import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.CONTENT;
import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.PROVIDER_ID;
import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.PROVIDER_NID;
import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.TARGET_SPACE;
import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.VERSION;

import java.util.List;

import javax.ws.rs.BeanParam;
import javax.ws.rs.QueryParam;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "configuration-filter")
public class ConfigurationFilterDto {

    @XmlElement(name = "provider-id")
    @QueryParam(PROVIDER_ID)
    private String providerId;

    @XmlElement(name = "content")
    @QueryParam(CONTENT)
    private List<String> content;

    @XmlElement(name = "provider-nid")
    @QueryParam(PROVIDER_NID)
    private String providerNid;

    @XmlElement(name = "target-space")
    @QueryParam(TARGET_SPACE)
    private String targetSpace;

    @BeanParam
    @XmlElement(name = "provider-target")
    private CloudTarget cloudTarget;

    @XmlElement(name = "provider-version")
    @QueryParam(VERSION)
    private String providerVersion;

    public ConfigurationFilterDto() {
        // Required by JAX-RS.
    }

    public ConfigurationFilterDto(String providerNid, String providerId, String version, CloudTarget cloudTarget, List<String> content) {
        this.providerId = providerId;
        this.providerVersion = version;
        this.content = content;
        this.providerNid = providerNid;
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
