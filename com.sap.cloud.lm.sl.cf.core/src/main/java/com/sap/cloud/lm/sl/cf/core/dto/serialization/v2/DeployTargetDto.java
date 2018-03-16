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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.PersistentObject;
import com.sap.cloud.lm.sl.common.model.json.MapWithNumbersAdapterFactory;
import com.sap.cloud.lm.sl.common.model.xml.PropertiesAdapter;
import com.sap.cloud.lm.sl.common.model.xml.Wrapper;
import com.sap.cloud.lm.sl.mta.model.v2_0.TargetModuleType;
import com.sap.cloud.lm.sl.mta.model.v2_0.TargetResourceType;
import com.sap.cloud.lm.sl.mta.model.v2_0.Target;

@XmlRootElement(name = "deployTarget")
@XmlAccessorType(value = javax.xml.bind.annotation.XmlAccessType.FIELD)
@XmlSeeAlso(Wrapper.class)
public class DeployTargetDto extends com.sap.cloud.lm.sl.cf.core.dto.serialization.DeployTargetDto<Target> {

    private static final DeployTargetModuleTypesAdapter MT_ADAPTER = new DeployTargetModuleTypesAdapter();
    private static final DeployTargetResourceTypesAdapter RT_ADAPTER = new DeployTargetResourceTypesAdapter();

    @XmlElement
    private long id;

    @Expose
    @XmlElement
    private String name;

    @Expose
    @XmlElement
    private String type;

    @Expose
    @XmlElement
    private String description;

    @Expose
    @JsonAdapter(MapWithNumbersAdapterFactory.class)
    @XmlJavaTypeAdapter(PropertiesAdapter.class)
    private Map<String, Object> parameters;

    @Expose
    @SerializedName("module-types")
    @XmlElementWrapper(name = "moduleTypes")
    @XmlElement(name = "moduleType")
    private List<DeployTargetModuleTypeDto> moduleTypes;

    @Expose
    @SerializedName("resource-types")
    @XmlElementWrapper(name = "resourceTypes")
    @XmlElement(name = "resourceType")
    private List<DeployTargetResourceTypeDto> resourceTypes;

    protected DeployTargetDto() {
        // Required by JAXB
    }

    public DeployTargetDto(PersistentObject<? extends Target> target) {
        this.id = target.getId();
        this.name = target.getObject()
            .getName();
        this.type = target.getObject()
            .getType();
        this.description = target.getObject()
            .getDescription();
        this.parameters = target.getObject()
            .getParameters();
        this.moduleTypes = MT_ADAPTER.marshal(target.getObject()
            .getModuleTypes2_0());
        this.resourceTypes = RT_ADAPTER.marshal(target.getObject()
            .getResourceTypes2_0());
    }

    @Override
    public PersistentObject<Target> toDeployTarget() {
        Target.Builder result = new Target.Builder();
        result.setName(name);
        result.setType(type);
        result.setDescription(description);
        result.setParameters(parameters);
        result.setModuleTypes2_0(MT_ADAPTER.unmarshal(moduleTypes));
        result.setResourceTypes2_0(RT_ADAPTER.unmarshal(resourceTypes));
        return new PersistentObject<>(id, result.build());
    }

}

class DeployTargetResourceTypesAdapter extends XmlAdapter<List<DeployTargetResourceTypeDto>, List<TargetResourceType>> {

    @Override
    public List<DeployTargetResourceTypeDto> marshal(List<TargetResourceType> resourceTypes) {
        if (resourceTypes == null) {
            return null;
        }

        List<DeployTargetResourceTypeDto> resourceTypeDtos = new ArrayList<>();
        for (TargetResourceType resourceType : resourceTypes) {
            resourceTypeDtos.add(new DeployTargetResourceTypeDto(resourceType));
        }

        return resourceTypeDtos;
    }

    @Override
    public List<TargetResourceType> unmarshal(List<DeployTargetResourceTypeDto> resourceTypeDtos) {
        if (resourceTypeDtos == null) {
            return null;
        }

        List<TargetResourceType> resourceTypes = new ArrayList<>();
        for (DeployTargetResourceTypeDto resourceTypeDto : resourceTypeDtos) {
            resourceTypes.add(resourceTypeDto.toTargetResourceType());
        }

        return resourceTypes;
    }

}

class DeployTargetModuleTypesAdapter extends XmlAdapter<List<DeployTargetModuleTypeDto>, List<TargetModuleType>> {

    @Override
    public List<DeployTargetModuleTypeDto> marshal(List<TargetModuleType> moduleTypes) {
        if (moduleTypes == null) {
            return null;
        }

        List<DeployTargetModuleTypeDto> moduleTypeDtos = new ArrayList<>();
        for (TargetModuleType moduleType : moduleTypes) {
            moduleTypeDtos.add(new DeployTargetModuleTypeDto(moduleType));
        }

        return moduleTypeDtos;
    }

    @Override
    public List<TargetModuleType> unmarshal(List<DeployTargetModuleTypeDto> moduleTypeDtos) {
        if (moduleTypeDtos == null) {
            return null;
        }

        List<TargetModuleType> moduleTypes = new ArrayList<>();
        for (DeployTargetModuleTypeDto moduleTypeDto : moduleTypeDtos) {
            moduleTypes.add(moduleTypeDto.toTargetModuleType());
        }

        return moduleTypes;
    }

}