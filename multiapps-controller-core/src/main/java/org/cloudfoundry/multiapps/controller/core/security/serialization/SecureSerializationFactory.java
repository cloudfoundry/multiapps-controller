package org.cloudfoundry.multiapps.controller.core.security.serialization;

import java.util.Collection;

public final class SecureSerializationFactory {

    private SecureSerializationFactory() {

    }

    public static DynamicSecureSerialization ofAdditionalValues(Collection<String> additionalSensitiveElementNames) {
        SecureSerializerConfiguration secureSerializerConfigurationWithAdditionalValues = new SecureSerializerConfiguration();

        secureSerializerConfigurationWithAdditionalValues.setAdditionalSensitiveElementNames(additionalSensitiveElementNames);
        return new DynamicSecureSerialization(secureSerializerConfigurationWithAdditionalValues);
    }

}
