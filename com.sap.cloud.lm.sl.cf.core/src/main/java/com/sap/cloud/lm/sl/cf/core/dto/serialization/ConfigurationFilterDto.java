package com.sap.cloud.lm.sl.cf.core.dto.serialization;

import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.CONTENT;
import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.PROVIDER_ID;
import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.PROVIDER_NID;
import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.TARGET_SPACE;
import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.VERSION;

import java.util.List;

import javax.ws.rs.QueryParam;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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

    @XmlElement(name = "provider-version")
    @QueryParam(VERSION)
    private String providerVersion;

    public ConfigurationFilterDto() {
        // Required by JAX-RS.
    }

    public ConfigurationFilterDto(String providerNid, String providerId, String version, String targetSpace, List<String> content) {
        this.providerId = providerId;
        this.providerVersion = version;
        this.content = content;
        this.providerNid = providerNid;
        this.targetSpace = targetSpace;
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

}
