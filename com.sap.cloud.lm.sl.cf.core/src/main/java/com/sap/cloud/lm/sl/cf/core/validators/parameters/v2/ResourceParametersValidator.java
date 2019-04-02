package com.sap.cloud.lm.sl.cf.core.validators.parameters.v2;

import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParametersValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParametersValidatorHelper;
import com.sap.cloud.lm.sl.mta.model.Resource;

public class ResourceParametersValidator extends ParametersValidator<Resource> {

    protected Resource resource;

    public ResourceParametersValidator(Resource resource, ParametersValidatorHelper helper) {
        super("", resource.getName(), helper, Resource.class);
        this.resource = resource;
    }

    @Override
    public Resource validate() {
        Map<String, Object> parameters = validateParameters(resource, resource.getParameters());
        resource.setParameters(parameters);
        return resource;
    }

}
