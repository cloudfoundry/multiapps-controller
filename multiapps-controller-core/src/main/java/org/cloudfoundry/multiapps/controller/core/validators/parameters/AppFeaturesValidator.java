package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.util.Map;

import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.model.Module;

public class AppFeaturesValidator implements ParameterValidator {

    @Override
    public boolean isValid(Object appFeaturesParameter, Map<String, Object> relatedParameters) {
        if (!(appFeaturesParameter instanceof Map)) {
            return false;
        }
        Map<String, Object> appFeatures = MiscUtil.cast(appFeaturesParameter);
        return appFeatures.values()
                          .stream()
                          .allMatch(Boolean.class::isInstance);
    }

    @Override
    public String getParameterName() {
        return SupportedParameters.APP_FEATURES;
    }

    @Override
    public Class<?> getContainerType() {
        return Module.class;
    }
}
