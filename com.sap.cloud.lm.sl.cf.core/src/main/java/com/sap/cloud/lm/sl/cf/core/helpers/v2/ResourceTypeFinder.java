package com.sap.cloud.lm.sl.cf.core.helpers.v2;

import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.mta.model.ElementContext;
import com.sap.cloud.lm.sl.mta.model.v2.PlatformResourceType;

public class ResourceTypeFinder extends com.sap.cloud.lm.sl.cf.core.helpers.v1.ResourceTypeFinder {

    public ResourceTypeFinder(String resourceType) {
        super(resourceType);
    }

    @Override
    public void visit(ElementContext context, com.sap.cloud.lm.sl.mta.model.v1.PlatformResourceType resourceType) {
        visit(context, (com.sap.cloud.lm.sl.mta.model.v2.PlatformResourceType) resourceType);
    }

    public void visit(ElementContext context, PlatformResourceType resourceType) {
        Map<String, Object> resourceTypeProperties = resourceType.getParameters();
        String typeProperty = (String) resourceTypeProperties.get(SupportedParameters.TYPE);
        if (typeProperty != null && typeProperty.equals(this.resourceType)) {
            resourceTypeName = resourceType.getName();
        }
    }
}
