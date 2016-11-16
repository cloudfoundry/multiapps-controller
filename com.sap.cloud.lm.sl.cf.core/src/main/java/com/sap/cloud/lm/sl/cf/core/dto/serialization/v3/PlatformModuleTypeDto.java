package com.sap.cloud.lm.sl.cf.core.dto.serialization.v3;

import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.google.gson.annotations.JsonAdapter;
import com.sap.cloud.lm.sl.common.model.json.PropertiesAdapterFactory;
import com.sap.cloud.lm.sl.common.model.xml.PropertiesAdapter;
import com.sap.cloud.lm.sl.mta.model.v3_1.PlatformModuleType;
import com.sap.cloud.lm.sl.mta.model.v3_1.PlatformModuleType.PlatformModuleTypeBuilder;

public class PlatformModuleTypeDto {

    @XmlElement
    private String name;

    @JsonAdapter(PropertiesAdapterFactory.class)
    @XmlJavaTypeAdapter(PropertiesAdapter.class)
    private Map<String, Object> properties;

    @JsonAdapter(PropertiesAdapterFactory.class)
    @XmlJavaTypeAdapter(PropertiesAdapter.class)
    private Map<String, Object> parameters;

    protected PlatformModuleTypeDto() {
        // Required by JAXB
    }

    public PlatformModuleTypeDto(PlatformModuleType moduleType) {
        this.name = moduleType.getName();
        this.properties = moduleType.getProperties();
        this.parameters = moduleType.getParameters();
    }

    public PlatformModuleType toPlatformModuleType() {
        PlatformModuleTypeBuilder result = new PlatformModuleTypeBuilder();
        result.setName(name);
        result.setProperties(properties);
        result.setParameters(parameters);
        return result.build();
    }
}
