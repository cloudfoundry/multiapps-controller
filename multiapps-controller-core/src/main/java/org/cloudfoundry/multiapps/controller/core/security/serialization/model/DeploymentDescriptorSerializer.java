package org.cloudfoundry.multiapps.controller.core.security.serialization.model;

import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureJsonSerializer;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerializerConfiguration;
import org.cloudfoundry.multiapps.controller.core.security.serialization.masking.DeploymentDescriptorMasker;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;

public class DeploymentDescriptorSerializer extends SecureJsonSerializer {

    private final DeploymentDescriptorMasker masker = new DeploymentDescriptorMasker();

    public DeploymentDescriptorSerializer(SecureSerializerConfiguration configuration) {
        super(configuration);
    }

    @Override
    public String serialize(Object object) {
        DeploymentDescriptor clonedDescriptor = DeploymentDescriptor.copyOf((DeploymentDescriptor) object);
        masker.mask(clonedDescriptor);
        return super.serialize(clonedDescriptor);
    }

}