package com.sap.cloud.lm.sl.cf.core.dto.serialization.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.sap.cloud.lm.sl.common.model.json.PropertiesAdapterFactory;
import com.sap.cloud.lm.sl.common.model.xml.PropertiesAdapter;
import com.sap.cloud.lm.sl.common.model.xml.Wrapper;
import com.sap.cloud.lm.sl.mta.model.v2_0.PlatformModuleType;
import com.sap.cloud.lm.sl.mta.model.v2_0.PlatformResourceType;
import com.sap.cloud.lm.sl.mta.model.v2_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v2_0.TargetPlatform.TargetPlatformBuilder;;

@XmlRootElement(name = "target-platform")
@XmlAccessorType(value = javax.xml.bind.annotation.XmlAccessType.FIELD)
@XmlSeeAlso(Wrapper.class)
public class TargetPlatformDto extends com.sap.cloud.lm.sl.cf.core.dto.serialization.TargetPlatformDto {

    private static final PlatformModuleTypesAdapter MT_ADAPTER = new PlatformModuleTypesAdapter();
    private static final PlatformResourceTypesAdapter RT_ADAPTER = new PlatformResourceTypesAdapter();

    @XmlElement
    private String name;
    @XmlElement
    private String type;
    @XmlElement
    private String description;

    @JsonAdapter(PropertiesAdapterFactory.class)
    @XmlJavaTypeAdapter(PropertiesAdapter.class)
    private Map<String, Object> parameters;

    @SerializedName("module-types")
    @XmlElementWrapper(name = "module-types")
    @XmlElement(name = "module-type")
    private List<PlatformModuleTypeDto> platformModuleTypes;

    @SerializedName("resource-types")
    @XmlElementWrapper(name = "resource-types")
    @XmlElement(name = "resource-type")
    private List<PlatformResourceTypeDto> platformResourceTypes;

    protected TargetPlatformDto() {
        // Required by JAXB
    }

    public TargetPlatformDto(TargetPlatform platform) {
        this.name = platform.getName();
        this.type = platform.getType();
        this.description = platform.getDescription();
        this.parameters = platform.getParameters();
        this.platformModuleTypes = MT_ADAPTER.marshal(platform.getModuleTypes2_0());
        this.platformResourceTypes = RT_ADAPTER.marshal(platform.getResourceTypes2_0());
    }

    @Override
    public TargetPlatform toTargetPlatform() {
        TargetPlatformBuilder result = new TargetPlatformBuilder();
        result.setName(name);
        result.setType(type);
        result.setDescription(description);
        result.setParameters(parameters);
        result.setModuleTypes2_0(MT_ADAPTER.unmarshal(platformModuleTypes));
        result.setResourceTypes2_0(RT_ADAPTER.unmarshal(platformResourceTypes));
        return result.build();
    }

}

class PlatformResourceTypesAdapter extends XmlAdapter<List<PlatformResourceTypeDto>, List<PlatformResourceType>> {

    @Override
    public List<PlatformResourceTypeDto> marshal(List<PlatformResourceType> resourceTypes) {
        if (resourceTypes == null) {
            return null;
        }

        List<PlatformResourceTypeDto> resourceTypeDtos = new ArrayList<PlatformResourceTypeDto>();
        for (PlatformResourceType resourceType : resourceTypes) {
            resourceTypeDtos.add(new PlatformResourceTypeDto(resourceType));
        }

        return resourceTypeDtos;
    }

    @Override
    public List<PlatformResourceType> unmarshal(List<PlatformResourceTypeDto> resourceTypeDtos) {
        if (resourceTypeDtos == null) {
            return null;
        }

        List<PlatformResourceType> resourceTypes = new ArrayList<PlatformResourceType>();
        for (PlatformResourceTypeDto resourceTypeDto : resourceTypeDtos) {
            resourceTypes.add(resourceTypeDto.toPlatformResourceType());
        }

        return resourceTypes;
    }

}

class PlatformModuleTypesAdapter extends XmlAdapter<List<PlatformModuleTypeDto>, List<PlatformModuleType>> {

    @Override
    public List<PlatformModuleTypeDto> marshal(List<PlatformModuleType> moduleTypes) {
        if (moduleTypes == null) {
            return null;
        }

        List<PlatformModuleTypeDto> moduleTypeDtos = new ArrayList<PlatformModuleTypeDto>();
        for (PlatformModuleType moduleType : moduleTypes) {
            moduleTypeDtos.add(new PlatformModuleTypeDto(moduleType));
        }

        return moduleTypeDtos;
    }

    @Override
    public List<PlatformModuleType> unmarshal(List<PlatformModuleTypeDto> moduleTypeDtos) {
        if (moduleTypeDtos == null) {
            return null;
        }

        List<PlatformModuleType> moduleTypes = new ArrayList<PlatformModuleType>();
        for (PlatformModuleTypeDto moduleTypeDto : moduleTypeDtos) {
            moduleTypes.add(moduleTypeDto.toPlatformModuleType());
        }

        return moduleTypes;
    }

}
