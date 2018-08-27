package com.sap.cloud.lm.sl.cf.core.validators.parameters.v2_0;

import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParameterValidator;
import com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2_0.Module;
import com.sap.cloud.lm.sl.mta.model.v2_0.Resource;

public class DescriptorParametersValidator extends com.sap.cloud.lm.sl.cf.core.validators.parameters.v1_0.DescriptorParametersValidator {

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
        List<com.sap.cloud.lm.sl.mta.model.v1_0.Module> modules = validateModules(descriptor.getModules1_0());
        List<com.sap.cloud.lm.sl.mta.model.v1_0.Resource> resources = validateResources(descriptor.getResources1_0());
        descriptor.setParameters(parameters);
        descriptor.setModules1_0(modules);
        descriptor.setResources1_0(resources);
    }

    @Override
    protected ResourceParametersValidator getResourceParametersValidator(com.sap.cloud.lm.sl.mta.model.v1_0.Resource resource) {
        return new ResourceParametersValidator((Resource) resource, helper);
    }

    @Override
    protected ModuleParametersValidator getModuleParametersValidator(com.sap.cloud.lm.sl.mta.model.v1_0.Module module) {
        return new ModuleParametersValidator((Module) module, helper);
    }

}
