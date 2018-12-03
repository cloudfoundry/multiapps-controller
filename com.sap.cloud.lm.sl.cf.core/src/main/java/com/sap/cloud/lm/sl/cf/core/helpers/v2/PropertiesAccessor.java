package com.sap.cloud.lm.sl.cf.core.helpers.v2;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.sap.cloud.lm.sl.mta.model.ParametersContainer;
import com.sap.cloud.lm.sl.mta.model.PropertiesContainer;

public class PropertiesAccessor {

    public Map<String, Object> getParameters(PropertiesContainer propertiesContainer, Set<String> supportedParameters) {
        return getParameters((ParametersContainer) propertiesContainer);
    }

    public Map<String, Object> getProperties(PropertiesContainer propertiesContainer, Set<String> supportedParameters) {
        return new TreeMap<>(propertiesContainer.getProperties());
    }

    public Map<String, Object> getParameters(ParametersContainer parametersContainer) {
        return new TreeMap<>(parametersContainer.getParameters());
    }

    public Map<String, Object> getParameters(PropertiesContainer propertiesContainer) {
        return getParameters(propertiesContainer);
    }

    public void setParameters(PropertiesContainer propertiesContainer, Map<String, Object> parameters) {
        setParameters((ParametersContainer) propertiesContainer, parameters);
    }

    public void setParameters(ParametersContainer parametersContainer, Map<String, Object> parameters) {
        parametersContainer.setParameters(parameters);
    }
    
    public void setProperties(PropertiesContainer propertiesContainer, Map<String, Object> properties) {
        propertiesContainer.setProperties(properties);
    }

}
