package org.cloudfoundry.multiapps.controller.core.security.serialization.model;

import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureJsonSerializer;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerializerConfiguration;
import org.cloudfoundry.multiapps.controller.core.security.serialization.masking.ResourceMasker;
import org.cloudfoundry.multiapps.mta.model.Resource;

public class ResourceSerializer extends SecureJsonSerializer {
    private final ResourceMasker masker = new ResourceMasker();

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
