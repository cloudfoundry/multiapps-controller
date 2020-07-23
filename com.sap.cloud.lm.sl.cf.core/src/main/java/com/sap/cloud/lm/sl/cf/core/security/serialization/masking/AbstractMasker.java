package com.sap.cloud.lm.sl.cf.core.security.serialization.masking;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudfoundry.multiapps.mta.model.Metadata;
import org.cloudfoundry.multiapps.mta.model.ParametersWithMetadataContainer;
import org.cloudfoundry.multiapps.mta.model.PropertiesWithMetadataContainer;

import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializerConfiguration;

public abstract class AbstractMasker<T> {

    public abstract void mask(T t);

    protected void maskParameters(ParametersWithMetadataContainer element) {
        Map<String, Object> maskedParametersMap = getMaskedProperties(element.getParameters(), element.getParametersMetadata());
        element.setParameters(maskedParametersMap);
    }

    protected void maskProperties(PropertiesWithMetadataContainer element) {
        Map<String, Object> maskedPropertiesMap = getMaskedProperties(element.getProperties(), element.getPropertiesMetadata());
        element.setProperties(maskedPropertiesMap);
    }

    protected Map<String, Object> getMaskedProperties(Map<String, Object> properties, Metadata metadata) {
        Map<String, Object> modifiablePropertiesMap = new HashMap<>(properties);
        maskSensitiveProperties(modifiablePropertiesMap, metadata);
        return modifiablePropertiesMap;
    }

    private void maskSensitiveProperties(Map<String, Object> properties, Metadata metadata) {
        for (Entry<String, Object> property : properties.entrySet()) {
            boolean isSensitive = metadata.getSensitiveMetadata(property.getKey());
            if (isSensitive) {
                maskSensitiveProperty(property);
            }
        }
    }

    private void maskSensitiveProperty(Entry<String, Object> property) {
        property.setValue(SecureSerializerConfiguration.SECURE_SERIALIZATION_MASK);
    }
}
