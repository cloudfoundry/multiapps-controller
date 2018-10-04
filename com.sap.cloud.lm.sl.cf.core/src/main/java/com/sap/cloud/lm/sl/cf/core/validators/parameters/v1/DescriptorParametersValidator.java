package com.sap.cloud.lm.sl.cf.core.validators.parameters.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParameterValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParametersValidator;
import com.sap.cloud.lm.sl.mta.model.v1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1.Module;
import com.sap.cloud.lm.sl.mta.model.v1.Resource;

public class DescriptorParametersValidator extends ParametersValidator<DeploymentDescriptor> {

    protected DeploymentDescriptor descriptor;

    public DescriptorParametersValidator(DeploymentDescriptor descriptor, List<ParameterValidator> parameterValidators) {
        this(descriptor, parameterValidators, false);
    }

    public DescriptorParametersValidator(DeploymentDescriptor descriptor, List<ParameterValidator> parameterValidators,
        boolean doNotCorrect) {
        super("", "", parameterValidators, DeploymentDescriptor.class, doNotCorrect);
        this.descriptor = descriptor;
    }

    @Override
    public DeploymentDescriptor validate() {
        Map<String, Object> properties = validateParameters(descriptor, descriptor.getProperties());
        List<Module> modules = validateModules(descriptor.getModules1());
        List<Resource> resources = validateResources(descriptor.getResources1());
        descriptor.setProperties(properties);
        descriptor.setModules1(modules);
        descriptor.setResources1(resources);
        return descriptor;
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

    protected ModuleParametersValidator getModuleParametersValidator(Module module) {
        return new ModuleParametersValidator(module, helper);
    }

}
