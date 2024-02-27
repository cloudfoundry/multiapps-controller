package com.sap.cloud.lm.sl.cf.core.validators.parameters.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParameterValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParametersValidator;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Module;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;

public class DescriptorParametersValidator extends ParametersValidator<DeploymentDescriptor> {

    protected DeploymentDescriptor descriptor;

    public DescriptorParametersValidator(DeploymentDescriptor deploymentDescriptor, List<ParameterValidator> parameterValidators) {
        this(deploymentDescriptor, parameterValidators, false);
    }

    public DescriptorParametersValidator(DeploymentDescriptor deploymentDescriptor, List<ParameterValidator> parameterValidators,
                                         boolean doNotCorrect) {
        super("", "", parameterValidators, DeploymentDescriptor.class, doNotCorrect);
        this.descriptor = deploymentDescriptor;
    }

    @Override
    public DeploymentDescriptor validate() {
        DeploymentDescriptor castedDescriptor = descriptor;
        validate(castedDescriptor);
        return castedDescriptor;
    }

    protected void validate(DeploymentDescriptor descriptor) {
        Map<String, Object> parameters = validateParameters(descriptor, descriptor.getParameters());
        List<Module> modules = validateModules(descriptor.getModules2());
        List<Resource> resources = validateResources(descriptor.getResources2());
        descriptor.setParameters(parameters);
        descriptor.setModules2(modules);
        descriptor.setResources2(resources);
    }

    protected List<Module> validateModules(List<Module> modules) {
        List<Module> validModules = new ArrayList<>();
        for (Module module : modules) {
            validModules.add(validate(module));
        }
        return validModules;
    }

    protected Module validate(Module module) {
        return getModuleParametersValidator(module).validate();
    }

    protected List<Resource> validateResources(List<Resource> resources) {
        List<Resource> validResources = new ArrayList<>();
        for (Resource resource : resources) {
            validResources.add(validate(resource));
        }
        return validResources;
    }

    protected Resource validate(Resource resource) {
        return getResourceParametersValidator(resource).validate();
    }

    protected ResourceParametersValidator getResourceParametersValidator(Resource resource) {
        return new ResourceParametersValidator(resource, helper);
    }

    protected ModuleParametersValidator getModuleParametersValidator(Module module) {
        return new ModuleParametersValidator((Module) module, helper);
    }

}
