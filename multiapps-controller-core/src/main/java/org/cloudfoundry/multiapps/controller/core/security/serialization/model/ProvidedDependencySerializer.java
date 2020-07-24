package org.cloudfoundry.multiapps.controller.core.security.serialization.model;

import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureJsonSerializer;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerializerConfiguration;
import org.cloudfoundry.multiapps.controller.core.security.serialization.masking.ProvidedDependencyMasker;
import org.cloudfoundry.multiapps.mta.model.ProvidedDependency;

public class ProvidedDependencySerializer extends SecureJsonSerializer {

    final ProvidedDependencyMasker masker = new ProvidedDependencyMasker();

    public ProvidedDependencySerializer(SecureSerializerConfiguration configuration) {
        super(configuration);
    }

    @Override
    public String serialize(Object object) {
        ProvidedDependency clonedDependency = ProvidedDependency.copyOf((ProvidedDependency) object);
        masker.mask(clonedDependency);
        return super.serialize(clonedDependency);
    }
}
