package com.sap.cloud.lm.sl.cf.core.dto.serialization.v2;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.sap.cloud.lm.sl.common.model.json.PropertiesAdapterFactory;
import com.sap.cloud.lm.sl.common.model.xml.PropertiesAdapter;
import com.sap.cloud.lm.sl.common.model.xml.Wrapper;
import com.sap.cloud.lm.sl.mta.model.v2_0.Target;
import com.sap.cloud.lm.sl.mta.model.v2_0.Target.TargetBuilder;

@XmlRootElement(name = "target-platform")
@XmlAccessorType(value = javax.xml.bind.annotation.XmlAccessType.FIELD)
@XmlSeeAlso(Wrapper.class)
@Deprecated
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

    public TargetPlatformDto(Target target) {
        this.name = target.getName();
        this.type = target.getType();
        this.description = target.getDescription();
        this.parameters = target.getParameters();
        this.platformModuleTypes = MT_ADAPTER.marshal(target.getModuleTypes2_0());
        this.platformResourceTypes = RT_ADAPTER.marshal(target.getResourceTypes2_0());
    }

    @Override
    public Target toTargetPlatform() {
        TargetBuilder result = new TargetBuilder();
        result.setName(name);
        result.setType(type);
        result.setDescription(description);
        result.setParameters(parameters);
        result.setModuleTypes2_0(MT_ADAPTER.unmarshal(platformModuleTypes));
        result.setResourceTypes2_0(RT_ADAPTER.unmarshal(platformResourceTypes));
        return result.build();
    }

}
