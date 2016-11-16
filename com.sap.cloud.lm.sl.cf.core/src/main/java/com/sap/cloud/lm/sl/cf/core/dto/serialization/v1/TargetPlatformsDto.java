package com.sap.cloud.lm.sl.cf.core.dto.serialization.v1;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;

@XmlAccessorType(value = javax.xml.bind.annotation.XmlAccessType.FIELD)
@XmlRootElement(name = "target-platforms")
public class TargetPlatformsDto extends com.sap.cloud.lm.sl.cf.core.dto.serialization.TargetPlatformsDto {

    @XmlJavaTypeAdapter(TargetPlatformAdapter.class)
    @XmlElement(name = "target-platform")
    private List<TargetPlatform> targetPlatforms;

    public TargetPlatformsDto() {
        // Required by JAXB
    }

    public TargetPlatformsDto(List<TargetPlatform> targetPlatforms) {
        this.targetPlatforms = targetPlatforms;
    }

    @Override
    public List<TargetPlatform> getTargetPlatforms() {
        return targetPlatforms;
    }

}

class TargetPlatformAdapter extends XmlAdapter<TargetPlatformDto, TargetPlatform> {

    @Override
    public TargetPlatform unmarshal(TargetPlatformDto dto) {
        return dto.toTargetPlatform();
    }

    @Override
    public TargetPlatformDto marshal(TargetPlatform platform) {
        return new TargetPlatformDto(platform);
    }

}
