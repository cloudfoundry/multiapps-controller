package com.sap.cloud.lm.sl.cf.core.dto.serialization.v1;

import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.sap.cloud.lm.sl.common.model.json.MapWithNumbersAdapterFactory;
import com.sap.cloud.lm.sl.common.model.xml.PropertiesAdapter;
import com.sap.cloud.lm.sl.mta.model.v1.TargetResourceType;

public class DeployTargetResourceTypeDto {

    @Expose
    @XmlElement
    protected String name;
    @Expose
    @JsonAdapter(MapWithNumbersAdapterFactory.class)
    @XmlJavaTypeAdapter(PropertiesAdapter.class)
    protected Map<String, Object> properties;

    protected DeployTargetResourceTypeDto() {
        // Required by JAXB
    }

    public DeployTargetResourceTypeDto(TargetResourceType resourceType) {
        name = resourceType.getName();
        properties = resourceType.getProperties();
    }

    public TargetResourceType toTargetResourceType() {
        TargetResourceType.Builder result = new TargetResourceType.Builder();
        result.setName(name);
        result.setProperties(properties);
        return result.build();
    }

}
