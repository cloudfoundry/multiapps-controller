package com.sap.cloud.lm.sl.cf.core.dto.serialization.v1;

import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.sap.cloud.lm.sl.common.model.json.MapWithNumbersAdapterFactory;
import com.sap.cloud.lm.sl.common.model.xml.PropertiesAdapter;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetModuleType;

public class DeployTargetModuleTypeDto {

    @Expose
    @XmlElement
    private String name;
    @Expose
    @JsonAdapter(MapWithNumbersAdapterFactory.class)
    @XmlJavaTypeAdapter(PropertiesAdapter.class)
    private Map<String, Object> properties;

    protected DeployTargetModuleTypeDto() {
        // Required by JAXB
    }

    public DeployTargetModuleTypeDto(TargetModuleType moduleType) {
        this.name = moduleType.getName();
        this.properties = moduleType.getProperties();
    }

    public TargetModuleType toTargetModuleType() {
        TargetModuleType.Builder result = new TargetModuleType.Builder();
        result.setName(name);
        result.setProperties(properties);
        return result.build();
    }

}
