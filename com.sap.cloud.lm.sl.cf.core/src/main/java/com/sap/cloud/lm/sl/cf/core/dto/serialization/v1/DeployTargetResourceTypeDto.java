package com.sap.cloud.lm.sl.cf.core.dto.serialization.v1;

import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.sap.cloud.lm.sl.common.model.json.PropertiesAdapterFactory;
import com.sap.cloud.lm.sl.common.model.xml.PropertiesAdapter;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetResourceType;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetResourceType.Builder;

public class DeployTargetResourceTypeDto {

    @Expose
    @XmlElement
    protected String name;
    @Expose
    @JsonAdapter(PropertiesAdapterFactory.class)
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
        Builder result = new Builder();
        result.setName(name);
        result.setProperties(properties);
        return result.build();
    }

}
