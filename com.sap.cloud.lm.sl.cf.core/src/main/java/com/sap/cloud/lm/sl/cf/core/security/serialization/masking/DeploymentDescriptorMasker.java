package com.sap.cloud.lm.sl.cf.core.security.serialization.masking;

import com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3_1.Module;
import com.sap.cloud.lm.sl.mta.model.v3_1.Resource;

public class DeploymentDescriptorMasker extends AbstractMasker<DeploymentDescriptor> {
    private ModuleMasker moduleMasker = new ModuleMasker();
    private ResourceMasker resourceMasker = new ResourceMasker();

    @Override
    public void mask(DeploymentDescriptor descriptor) {
        maskParameters(descriptor);
        for (Module module : descriptor.getModules3_1()) {
            moduleMasker.mask(module);
        }
        for (Resource resource : descriptor.getResources3_1()) {
            resourceMasker.mask(resource);
        }
    }
}
