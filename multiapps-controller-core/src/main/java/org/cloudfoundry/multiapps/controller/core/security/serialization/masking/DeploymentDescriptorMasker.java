package org.cloudfoundry.multiapps.controller.core.security.serialization.masking;

import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.Resource;

public class DeploymentDescriptorMasker extends AbstractMasker<DeploymentDescriptor> {
    private final ModuleMasker moduleMasker = new ModuleMasker();
    private final ResourceMasker resourceMasker = new ResourceMasker();

    @Override
    public void mask(DeploymentDescriptor descriptor) {
        maskParameters(descriptor);
        for (Module module : descriptor.getModules()) {
            moduleMasker.mask(module);
        }
        for (Resource resource : descriptor.getResources()) {
            resourceMasker.mask(resource);
        }
    }
}
