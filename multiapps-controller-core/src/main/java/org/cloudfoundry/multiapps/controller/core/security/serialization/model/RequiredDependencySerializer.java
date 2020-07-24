package org.cloudfoundry.multiapps.controller.core.security.serialization.model;

import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureJsonSerializer;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerializerConfiguration;
import org.cloudfoundry.multiapps.controller.core.security.serialization.masking.RequiredDependencyMasker;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;

public class RequiredDependencySerializer extends SecureJsonSerializer {

    private final RequiredDependencyMasker masker = new RequiredDependencyMasker();

    public RequiredDependencySerializer(SecureSerializerConfiguration configuration) {
        super(configuration);
    }

    @Override
    public String serialize(Object object) {
        RequiredDependency clonedDependency = RequiredDependency.copyOf((RequiredDependency) object);
        masker.mask(clonedDependency);
        return super.serialize(clonedDependency);
    }

}
