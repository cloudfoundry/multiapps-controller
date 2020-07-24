package org.cloudfoundry.multiapps.controller.core.validators.parameters.v2;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.validators.parameters.ParametersValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.ParametersValidatorHelper;
import org.cloudfoundry.multiapps.mta.model.Resource;

public class ResourceParametersValidator extends ParametersValidator<Resource> {

    protected final Resource resource;

    public ResourceParametersValidator(Resource resource, ParametersValidatorHelper helper) {
        super("", resource.getName(), helper, Resource.class);
        this.resource = resource;
    }

    @Override
    public Resource validate() {
        Map<String, Object> parameters = validateParameters(resource.getParameters());
        resource.setParameters(parameters);
        return resource;
    }

}
