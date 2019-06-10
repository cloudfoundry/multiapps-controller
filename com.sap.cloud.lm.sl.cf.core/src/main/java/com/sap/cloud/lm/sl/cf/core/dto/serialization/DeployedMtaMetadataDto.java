package com.sap.cloud.lm.sl.cf.core.dto.serialization;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import com.sap.cloud.lm.sl.cf.core.model.MtaMetadata;
import com.sap.cloud.lm.sl.mta.model.Version;

@XmlAccessorType(XmlAccessType.FIELD)
public class DeployedMtaMetadataDto {

    private String id;
    private String version;

    protected DeployedMtaMetadataDto() {
        // Required by JAXB
    }

    public DeployedMtaMetadataDto(MtaMetadata metadata) {
        this.id = metadata.getId();
        this.version = metadata.getVersion()
                               .toString();
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public MtaMetadata toDeployedMtaMetadata() {
        MtaMetadata result = new MtaMetadata();
        result.setId(id);
        result.setVersion(Version.parseVersion(version));
        return result;
    }

}
