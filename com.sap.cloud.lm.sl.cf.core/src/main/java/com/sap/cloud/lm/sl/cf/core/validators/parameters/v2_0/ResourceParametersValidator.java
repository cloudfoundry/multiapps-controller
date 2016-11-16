package com.sap.cloud.lm.sl.cf.core.validators.parameters.v2_0;

import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParametersValidatorHelper;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.v2_0.Resource;;

public class ResourceParametersValidator extends com.sap.cloud.lm.sl.cf.core.validators.parameters.v1_0.ResourceParametersValidator {

    public ResourceParametersValidator(Resource resource, ParametersValidatorHelper helper) {
        super(resource, helper);
    }

    @Override
    public Resource validate() throws SLException {
        Resource resourceV2 = (Resource) resource;
        Map<String, Object> parameters = validateParameters(resource, ((Resource) resource).getParameters());
        resourceV2.setParameters(parameters);
        return resourceV2;
    }

}
