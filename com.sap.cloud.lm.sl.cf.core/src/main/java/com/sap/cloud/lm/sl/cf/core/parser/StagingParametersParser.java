package com.sap.cloud.lm.sl.cf.core.parser;

import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPropertyValue;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.Staging;

import com.sap.cloud.lm.sl.cf.client.lib.domain.StagingExtended;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;

public class StagingParametersParser implements ParametersParser<Staging> {

    private static final String DEFAULT_HEALTH_CHECK_HTTP_ENDPOINT = "/";
    private static final String HTTP_HEALTH_CHECK_TYPE = "http";

    @Override
    public Staging parse(List<Map<String, Object>> parametersList) {
        String command = (String) getPropertyValue(parametersList, SupportedParameters.COMMAND, null);
        String buildpack = (String) getPropertyValue(parametersList, SupportedParameters.BUILDPACK, null);
        String stack = (String) getPropertyValue(parametersList, SupportedParameters.STACK, null);
        Integer healthCheckTimeout = (Integer) getPropertyValue(parametersList, SupportedParameters.HEALTH_CHECK_TIMEOUT, null);
        String healthCheckType = (String) getPropertyValue(parametersList, SupportedParameters.HEALTH_CHECK_TYPE, null);
        String healthCheckHttpEndpoint = (String) getPropertyValue(parametersList, SupportedParameters.HEALTH_CHECK_HTTP_ENDPOINT,
            getDefaultHealthCheckHttpEndpoint(healthCheckType));
        return new StagingExtended(command, buildpack, stack, healthCheckTimeout, healthCheckType, healthCheckHttpEndpoint);
    }

    private String getDefaultHealthCheckHttpEndpoint(String healthCheckType) {
        return HTTP_HEALTH_CHECK_TYPE.equals(healthCheckType) ? DEFAULT_HEALTH_CHECK_HTTP_ENDPOINT : null;
    }

}
