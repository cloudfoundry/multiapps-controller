package com.sap.cloud.lm.sl.cf.core.helpers.v1_0;

import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.mta.model.ElementContext;
import com.sap.cloud.lm.sl.mta.model.Visitor;
import com.sap.cloud.lm.sl.mta.model.v1_0.ResourceType;

public class ResourceTypeFinder extends Visitor {

    protected String resourceTypeName = null;
    protected String resourceType;

    public ResourceTypeFinder(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceTypeName() {
        return resourceTypeName;
    }

    @Override
    public void visit(ElementContext context, ResourceType resourceType) {
        Map<String, Object> resourceTypeProperties = resourceType.getProperties();
        String typeProperty = (String) resourceTypeProperties.get(SupportedParameters.TYPE);
        if (typeProperty != null && typeProperty.equals(this.resourceType)) {
            resourceTypeName = resourceType.getName();
        }
    }
}
