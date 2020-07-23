package com.sap.cloud.lm.sl.cf.core.security.serialization.model;

import org.cloudfoundry.multiapps.mta.model.ProvidedDependency;

import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureJsonSerializer;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializerConfiguration;
import com.sap.cloud.lm.sl.cf.core.security.serialization.masking.ProvidedDependencyMasker;

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
