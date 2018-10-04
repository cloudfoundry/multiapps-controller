package com.sap.cloud.lm.sl.cf.core.validators.parameters.v1;

import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParametersValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParametersValidatorHelper;
import com.sap.cloud.lm.sl.mta.model.v1.Resource;

public class ResourceParametersValidator extends ParametersValidator<Resource> {

    protected Resource resource;

    public ResourceParametersValidator(Resource resource, ParametersValidatorHelper helper) {
        super("", resource.getName(), helper, Resource.class);
        this.resource = resource;
    }

    @Override
    public Resource validate() {
        Map<String, Object> properties = validateParameters(resource, resource.getProperties());
        resource.setProperties(properties);
        return resource;
    }

}
