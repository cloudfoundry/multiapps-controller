package com.sap.cloud.lm.sl.cf.core.parser;

import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPropertyValue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.client.lib.domain.RestartParameters;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;

public class RestartParametersParser implements ParametersParser<RestartParameters> {

    @Override
    public RestartParameters parse(List<Map<String, Object>> parametersList) {
        Map<String, Boolean> restartParameters = getRestartParametersFromDescriptor(parametersList);
        boolean shouldRestartOnVcapAppChange = restartParameters.getOrDefault(SupportedParameters.VCAP_APPLICATION_ENV, true);
        boolean shouldRestartOnVcapServicesChange = restartParameters.getOrDefault(SupportedParameters.VCAP_SERVICES_ENV, true);
        boolean shouldRestartOnUserProvidedChange = restartParameters.getOrDefault(SupportedParameters.USER_PROVIDED_ENV, true);
        return new RestartParameters(shouldRestartOnVcapAppChange, shouldRestartOnVcapServicesChange, shouldRestartOnUserProvidedChange);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Boolean> getRestartParametersFromDescriptor(List<Map<String, Object>> parametersList) {
        return (Map<String, Boolean>) getPropertyValue(parametersList, SupportedParameters.RESTART_ON_ENV_CHANGE, Collections.emptyMap());
    }

}
