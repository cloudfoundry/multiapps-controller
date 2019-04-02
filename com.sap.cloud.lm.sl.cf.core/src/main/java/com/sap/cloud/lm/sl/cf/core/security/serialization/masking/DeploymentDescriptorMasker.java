package com.sap.cloud.lm.sl.cf.core.security.serialization.masking;

import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Module;
import com.sap.cloud.lm.sl.mta.model.Resource;

public class DeploymentDescriptorMasker extends AbstractMasker<DeploymentDescriptor> {
    private ModuleMasker moduleMasker = new ModuleMasker();
    private ResourceMasker resourceMasker = new ResourceMasker();

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
