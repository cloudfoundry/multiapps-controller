package com.sap.cloud.lm.sl.cf.core.validators.parameters.v2;

import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParametersValidatorHelper;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;

public class ResourceParametersValidator extends com.sap.cloud.lm.sl.cf.core.validators.parameters.v1.ResourceParametersValidator {

    public ResourceParametersValidator(Resource resource, ParametersValidatorHelper helper) {
        super(resource, helper);
    }

    @Override
    public Resource validate() {
        Resource resourceV2 = (Resource) resource;
        Map<String, Object> parameters = validateParameters(resource, ((Resource) resource).getParameters());
        resourceV2.setParameters(parameters);
        return resourceV2;
    }

}
