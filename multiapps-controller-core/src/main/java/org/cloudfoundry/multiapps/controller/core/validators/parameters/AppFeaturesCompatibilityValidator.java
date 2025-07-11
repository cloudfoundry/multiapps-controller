package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.Constants;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;

public class AppFeaturesCompatibilityValidator implements CompatibilityParameterValidator {

    private final Map<String, Object> parameters;

    public AppFeaturesCompatibilityValidator(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    @Override
    public boolean isCompatible(String parameter) {
        if (getIncompatibleParameters().contains(parameter)) {
            Object appFeaturesObj = parameters.get(SupportedParameters.APP_FEATURES);
            if (appFeaturesObj instanceof Map<?, ?> appFeatures) {
                return !appFeatures.containsKey(Constants.APP_FEATURE_SSH);
            }
        }
        return true;
    }

    @Override
    public String getParameterName() {
        return SupportedParameters.APP_FEATURES;
    }

    @Override
    public List<String> getIncompatibleParameters() {
        return List.of(SupportedParameters.ENABLE_SSH);
    }
}
