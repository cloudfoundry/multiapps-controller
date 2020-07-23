package com.sap.cloud.lm.sl.cf.core.security.serialization.model;

import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;

import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureJsonSerializer;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializerConfiguration;
import com.sap.cloud.lm.sl.cf.core.security.serialization.masking.DeploymentDescriptorMasker;

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