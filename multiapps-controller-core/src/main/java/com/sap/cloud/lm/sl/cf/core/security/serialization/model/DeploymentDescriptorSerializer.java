package com.sap.cloud.lm.sl.cf.core.security.serialization.model;

import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureJsonSerializer;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializerConfiguration;
import com.sap.cloud.lm.sl.cf.core.security.serialization.masking.DeploymentDescriptorMasker;
import com.sap.cloud.lm.sl.mta.model.v3.DeploymentDescriptor;

public class DeploymentDescriptorSerializer extends SecureJsonSerializer {

    private DeploymentDescriptorMasker masker = new DeploymentDescriptorMasker();

    public DeploymentDescriptorSerializer(SecureSerializerConfiguration configuration) {
        super(configuration);
    }

    @Override
    public String serialize(Object object) {
        DeploymentDescriptor clonedDescriptor = ((DeploymentDescriptor) object).copyOf();
        masker.mask(clonedDescriptor);
        return super.serialize(clonedDescriptor);
    }

}