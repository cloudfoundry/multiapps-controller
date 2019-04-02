package com.sap.cloud.lm.sl.cf.core.security.serialization.model;

import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureJsonSerializer;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializerConfiguration;
import com.sap.cloud.lm.sl.cf.core.security.serialization.masking.DeploymentDescriptorMasker;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;

public class DeploymentDescriptorSerializer extends SecureJsonSerializer {

    private DeploymentDescriptorMasker masker = new DeploymentDescriptorMasker();

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