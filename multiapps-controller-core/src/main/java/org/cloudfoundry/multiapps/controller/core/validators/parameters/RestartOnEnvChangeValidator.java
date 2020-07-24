package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.util.Map;

import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.model.Module;

public class RestartOnEnvChangeValidator implements ParameterValidator {

    @Override
    public boolean isValid(Object restartParameters, final Map<String, Object> context) {
        if (!(restartParameters instanceof Map)) {
            return false;
        }
        Map<String, Object> parameters = MiscUtil.cast(restartParameters);
        if (parameters.containsKey(SupportedParameters.VCAP_APPLICATION_ENV)
            && !isValidBooleanParameter(parameters.get(SupportedParameters.VCAP_APPLICATION_ENV))) {
            return false;
        }
        if (parameters.containsKey(SupportedParameters.VCAP_SERVICES_ENV)
            && !isValidBooleanParameter(parameters.get(SupportedParameters.VCAP_SERVICES_ENV))) {
            return false;
        }
        return !parameters.containsKey(SupportedParameters.USER_PROVIDED_ENV)
            || isValidBooleanParameter(parameters.get(SupportedParameters.USER_PROVIDED_ENV));
    }

    private boolean isValidBooleanParameter(Object parameter) {
        return parameter instanceof Boolean;
    }

    @Override
    public Class<?> getContainerType() {
        return Module.class;
    }

    @Override
    public String getParameterName() {
        return SupportedParameters.RESTART_ON_ENV_CHANGE;
    }

}
