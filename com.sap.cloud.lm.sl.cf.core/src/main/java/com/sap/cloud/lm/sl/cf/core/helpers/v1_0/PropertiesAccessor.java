package com.sap.cloud.lm.sl.cf.core.helpers.v1_0;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.sap.cloud.lm.sl.mta.model.PropertiesContainer;

public class PropertiesAccessor {

    public Map<String, Object> getProperties(PropertiesContainer propertiesContainer, Set<String> supportedParameters) {
        return getOnlyProperties(propertiesContainer.getProperties(), supportedParameters);
    }

    public Map<String, Object> getParameters(PropertiesContainer propertiesContainer, Set<String> supportedParameters) {
        return getOnlyParameters(propertiesContainer.getProperties(), supportedParameters);
    }

    public Map<String, Object> getProperties(PropertiesContainer propertiesContainer) {
        return new TreeMap<>(propertiesContainer.getProperties());
    }

    public Map<String, Object> getParameters(PropertiesContainer propertiesContainer) {
        return new TreeMap<>(propertiesContainer.getProperties());
    }

    private Map<String, Object> getOnlyProperties(Map<String, Object> properties, Set<String> supportedParameters) {
        Map<String, Object> result = new TreeMap<>(properties);
        result.keySet().removeAll(supportedParameters);
        return result;
    }

    private Map<String, Object> getOnlyParameters(Map<String, Object> properties, Set<String> supportedParameters) {
        Map<String, Object> result = new TreeMap<>(properties);
        result.keySet().retainAll(supportedParameters);
        return result;
    }

    public void setProperties(PropertiesContainer propertiesContainer, Map<String, Object> properties) {
        propertiesContainer.setProperties(properties);
    }

    public void setParameters(PropertiesContainer propertiesContainer, Map<String, Object> properties) {
        propertiesContainer.setProperties(properties);
    }

}
