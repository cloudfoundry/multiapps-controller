package com.sap.cloud.lm.sl.cf.core.validators.parameters.v2;

import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;

import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.CompatabilityParametersValidator;

public class DescriptorParametersCompatabilityValidator extends CompatabilityParametersValidator<DeploymentDescriptor> {

    private final DeploymentDescriptor descriptor;

    public DescriptorParametersCompatabilityValidator(DeploymentDescriptor descriptor, UserMessageLogger userMessageLogger) {
        super(userMessageLogger);
        this.descriptor = descriptor;
    }

    @Override
    public DeploymentDescriptor validate() {
        DeploymentDescriptor castedDescriptor = this.descriptor;
        validate(castedDescriptor);
        return castedDescriptor;
    }

    private void validate(DeploymentDescriptor descriptor) {
        List<Module> modules = validateModules(descriptor.getModules());
        descriptor.setModules(modules);
    }

    private List<Module> validateModules(List<Module> modules) {
        return modules.stream()
                      .map(this::validate)
                      .collect(Collectors.toList());
    }

    private Module validate(Module module) {
        return getModuleParametersCompatabilityValidator(module).validate();
    }

    protected ModuleParametersCompatabilityValidator getModuleParametersCompatabilityValidator(Module module) {
        return new ModuleParametersCompatabilityValidator(module, userMessageLogger);
    }

}
