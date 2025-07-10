package org.cloudfoundry.multiapps.controller.core.validators.parameters.v2;

import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.CompatibilityParametersValidator;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;

public class DescriptorParametersCompatibilityValidator extends CompatibilityParametersValidator<DeploymentDescriptor> {

    private final DeploymentDescriptor descriptor;

    public DescriptorParametersCompatibilityValidator(DeploymentDescriptor descriptor, UserMessageLogger userMessageLogger) {
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
        return getModuleParametersCompatibilityValidator(module).validate();
    }

    protected ModuleParametersCompatibilityValidator getModuleParametersCompatibilityValidator(Module module) {
        return new ModuleParametersCompatibilityValidator(module, userMessageLogger);
    }

}
