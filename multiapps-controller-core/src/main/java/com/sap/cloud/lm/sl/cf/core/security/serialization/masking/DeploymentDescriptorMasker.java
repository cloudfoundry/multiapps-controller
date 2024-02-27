package com.sap.cloud.lm.sl.cf.core.security.serialization.masking;

import com.sap.cloud.lm.sl.mta.model.v3.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3.Module;
import com.sap.cloud.lm.sl.mta.model.v3.Resource;

public class DeploymentDescriptorMasker extends AbstractMasker<DeploymentDescriptor> {
    private ModuleMasker moduleMasker = new ModuleMasker();
    private ResourceMasker resourceMasker = new ResourceMasker();

    @Override
    public void mask(DeploymentDescriptor descriptor) {
        maskParameters(descriptor);
        for (Module module : descriptor.getModules3()) {
            moduleMasker.mask(module);
        }
        for (Resource resource : descriptor.getResources3()) {
            resourceMasker.mask(resource);
        }
    }
}
