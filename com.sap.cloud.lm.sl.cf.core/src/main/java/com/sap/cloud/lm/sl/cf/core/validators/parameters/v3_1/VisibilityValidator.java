package com.sap.cloud.lm.sl.cf.core.validators.parameters.v3_1;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParameterValidator;
import com.sap.cloud.lm.sl.common.util.CommonUtil;
import com.sap.cloud.lm.sl.mta.model.v3_1.ProvidedDependency;

public class VisibilityValidator implements ParameterValidator {

    @Override
    public boolean isValid(Object visibleTargets) {
        if (!(visibleTargets instanceof List)) {
            return false;
        }
        List<Map<String, String>> targets = CommonUtil.cast(visibleTargets);
        for (Map<String, String> target : targets) {
            if (!isValidTarget(target)) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidTarget(Map<String, String> target) {
        for (Entry<String, String> entry : target.entrySet()) {
            if (!(entry.getKey() instanceof String) && !(entry.getValue() instanceof String)) {
                return false;
            }
        }
        if (!target.containsKey(SupportedParameters.ORG) || !(target.get(SupportedParameters.ORG) instanceof String)) {
            return false;
        }
        if (target.containsKey(SupportedParameters.SPACE) && !(target.get(SupportedParameters.SPACE) instanceof String)) {
            return false;
        }
        return true;
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
