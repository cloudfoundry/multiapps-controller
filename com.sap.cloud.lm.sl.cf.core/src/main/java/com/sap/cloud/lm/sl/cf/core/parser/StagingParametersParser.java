package com.sap.cloud.lm.sl.cf.core.parser;

import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPropertyValue;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.Staging;

import com.sap.cloud.lm.sl.cf.client.lib.domain.StagingExtended;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;

public class StagingParametersParser implements ParametersParser<Staging> {

    private static final String DEFAULT_HEALTH_CHECK_TYPE = null;

    @Override
    public Staging parse(List<Map<String, Object>> parametersList) {
        String command = (String) getPropertyValue(parametersList, SupportedParameters.COMMAND, null);
        String buildpack = (String) getPropertyValue(parametersList, SupportedParameters.BUILDPACK, null);
        String stack = (String) getPropertyValue(parametersList, SupportedParameters.STACK, null);
        Integer healthCheckTimeout = (Integer) getPropertyValue(parametersList, SupportedParameters.HEALTH_CHECK_TIMEOUT, null);
        String healthCheckType = (String) getPropertyValue(parametersList, SupportedParameters.HEALTH_CHECK_TYPE,
            DEFAULT_HEALTH_CHECK_TYPE);
        return new StagingExtended(command, buildpack, stack, healthCheckTimeout, healthCheckType);
    }

}
