package com.sap.cloud.lm.sl.cf.core.helpers.v2;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.sap.cloud.lm.sl.mta.model.ParametersContainer;
import com.sap.cloud.lm.sl.mta.model.PropertiesContainer;

public class PropertiesAccessor {

    public Map<String, Object> getParameters(PropertiesContainer propertiesContainer, Set<String> supportedParameters) {
        return new TreeMap<>(((ParametersContainer) propertiesContainer).getParameters());
    }

    public Map<String, Object> getProperties(PropertiesContainer propertiesContainer, Set<String> supportedParameters) {
        return new TreeMap<>(propertiesContainer.getProperties());
    }

    public Map<String, Object> getProperties(PropertiesContainer propertiesContainer) {
        return new TreeMap<>(propertiesContainer.getProperties());
    }

    public Map<String, Object> getParameters(PropertiesContainer propertiesContainer) {
        return new TreeMap<>(((ParametersContainer) propertiesContainer).getParameters());
    }

    public void setParameters(PropertiesContainer propertiesContainer, Map<String, Object> parameters) {
        ((ParametersContainer) propertiesContainer).setParameters(parameters);
    }

}
