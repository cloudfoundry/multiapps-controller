package com.sap.cloud.lm.sl.cf.core.security.serialization.model;

import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureJsonSerializer;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializerConfiguration;
import com.sap.cloud.lm.sl.cf.core.security.serialization.masking.ResourceMasker;
import com.sap.cloud.lm.sl.mta.model.Resource;

public class ResourceSerializer extends SecureJsonSerializer {
    private ResourceMasker masker = new ResourceMasker();

    public ResourceSerializer(SecureSerializerConfiguration configuration) {
        super(configuration);
    }

    @Override
    public String serialize(Object object) {
        Resource clonedResource = Resource.copyOf((Resource) object);
        masker.mask(clonedResource);
        return super.serialize(clonedResource);
    }
}
