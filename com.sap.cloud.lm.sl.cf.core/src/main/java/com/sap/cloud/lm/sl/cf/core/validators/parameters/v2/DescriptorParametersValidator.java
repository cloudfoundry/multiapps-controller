package com.sap.cloud.lm.sl.cf.core.validators.parameters.v2;

import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParameterValidator;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Module;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;

public class DescriptorParametersValidator extends com.sap.cloud.lm.sl.cf.core.validators.parameters.v1.DescriptorParametersValidator {

    public DescriptorParametersValidator(DeploymentDescriptor deploymentDescriptor, List<ParameterValidator> parameterValidators) {
        super(deploymentDescriptor, parameterValidators);
    }

    public DescriptorParametersValidator(DeploymentDescriptor deploymentDescriptor, List<ParameterValidator> parameterValidators,
        boolean doNotCorrect) {
        super(deploymentDescriptor, parameterValidators, doNotCorrect);
    }

    @Override
    public DeploymentDescriptor validate() {
        DeploymentDescriptor castedDescriptor = (DeploymentDescriptor) descriptor;
        validate(castedDescriptor);
        return castedDescriptor;
    }

    protected void validate(DeploymentDescriptor descriptor) {
        Map<String, Object> parameters = validateParameters(descriptor, descriptor.getParameters());
        List<com.sap.cloud.lm.sl.mta.model.v1.Module> modules = validateModules(descriptor.getModules1());
        List<com.sap.cloud.lm.sl.mta.model.v1.Resource> resources = validateResources(descriptor.getResources1());
        descriptor.setParameters(parameters);
        descriptor.setModules1(modules);
        descriptor.setResources1(resources);
    }

    @Override
    protected ResourceParametersValidator getResourceParametersValidator(com.sap.cloud.lm.sl.mta.model.v1.Resource resource) {
        return new ResourceParametersValidator((Resource) resource, helper);
    }

    @Override
    protected ModuleParametersValidator getModuleParametersValidator(com.sap.cloud.lm.sl.mta.model.v1.Module module) {
        return new ModuleParametersValidator((Module) module, helper);
    }

}
