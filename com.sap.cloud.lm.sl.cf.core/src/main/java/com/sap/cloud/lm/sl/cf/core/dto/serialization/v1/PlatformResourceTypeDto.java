package com.sap.cloud.lm.sl.cf.core.dto.serialization.v1;

import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.sap.cloud.lm.sl.common.model.json.PropertiesAdapterFactory;
import com.sap.cloud.lm.sl.common.model.xml.PropertiesAdapter;
import com.sap.cloud.lm.sl.mta.model.v1_0.PlatformResourceType;
import com.sap.cloud.lm.sl.mta.model.v1_0.PlatformResourceType.PlatformResourceTypeBuilder;

public class PlatformResourceTypeDto {
    @Expose
    @XmlElement
    protected String name;
    @Expose
    @JsonAdapter(PropertiesAdapterFactory.class)
    @XmlJavaTypeAdapter(PropertiesAdapter.class)
    protected Map<String, Object> properties;

    protected PlatformResourceTypeDto() {
        // Required by JAXB
    }

    public PlatformResourceTypeDto(PlatformResourceType resourceType) {
        name = resourceType.getName();
        properties = resourceType.getProperties();
    }

    public PlatformResourceType toPlatformResourceType() {
        PlatformResourceTypeBuilder result = new PlatformResourceTypeBuilder();
        result.setName(name);
        result.setProperties(properties);
        return result.build();
    }

}
