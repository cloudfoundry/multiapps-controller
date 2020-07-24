package org.cloudfoundry.multiapps.controller.core.validators.parameters.v3;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.ParameterValidator;
import org.cloudfoundry.multiapps.mta.model.ProvidedDependency;

public class VisibilityValidator implements ParameterValidator {

    @Override
    public boolean isValid(Object visibleTargets, final Map<String, Object> context) {
        if (!(visibleTargets instanceof List)) {
            return false;
        }
        List<Map<String, String>> targets = MiscUtil.cast(visibleTargets);
        for (Map<String, String> target : targets) {
            if (!isValidTarget(target)) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidTarget(Map<String, String> target) {
        for (Entry<String, String> entry : target.entrySet()) {
            if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof String)) {
                return false;
            }
        }
        return target.containsKey(SupportedParameters.ORGANIZATION_NAME);
    }

    @Override
    public Class<?> getContainerType() {
        return ProvidedDependency.class;
    }

    @Override
    public String getParameterName() {
        return SupportedParameters.VISIBILITY;
    }

}
