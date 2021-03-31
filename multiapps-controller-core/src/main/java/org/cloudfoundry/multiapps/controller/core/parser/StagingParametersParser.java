package org.cloudfoundry.multiapps.controller.core.parser;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.util.PropertiesUtil;

import com.sap.cloudfoundry.client.facade.domain.DockerInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableStaging;
import com.sap.cloudfoundry.client.facade.domain.Staging;

public class StagingParametersParser implements ParametersParser<Staging> {

    private static final String DEFAULT_HEALTH_CHECK_HTTP_ENDPOINT = "/";
    private static final String HTTP_HEALTH_CHECK_TYPE = "http";

    @Override
    public Staging parse(List<Map<String, Object>> parametersList) {
        String command = (String) PropertiesUtil.getPropertyValue(parametersList, SupportedParameters.COMMAND, null);
        List<String> buildpacks = PropertiesUtil.getPluralOrSingular(parametersList, SupportedParameters.BUILDPACKS,
                                                                     SupportedParameters.BUILDPACK);
        String stack = (String) PropertiesUtil.getPropertyValue(parametersList, SupportedParameters.STACK, null);
        Integer healthCheckTimeout = (Integer) PropertiesUtil.getPropertyValue(parametersList, SupportedParameters.HEALTH_CHECK_TIMEOUT,
                                                                               null);
        Integer healthCheckInvocationTimeout = (Integer) PropertiesUtil.getPropertyValue(parametersList,
                                                                                         SupportedParameters.HEALTH_CHECK_INVOCATION_TIMEOUT,
                                                                                         null);
        String healthCheckType = (String) PropertiesUtil.getPropertyValue(parametersList, SupportedParameters.HEALTH_CHECK_TYPE, null);
        String healthCheckHttpEndpoint = (String) PropertiesUtil.getPropertyValue(parametersList,
                                                                                  SupportedParameters.HEALTH_CHECK_HTTP_ENDPOINT,
                                                                                  getDefaultHealthCheckHttpEndpoint(healthCheckType));
        Boolean isSshEnabled = (Boolean) PropertiesUtil.getPropertyValue(parametersList, SupportedParameters.ENABLE_SSH, null);
        DockerInfo dockerInfo = new DockerInfoParser().parse(parametersList);
        return ImmutableStaging.builder()
                               .command(command)
                               .buildpacks(buildpacks)
                               .stack(stack)
                               .healthCheckTimeout(healthCheckTimeout)
                               .invocationTimeout(healthCheckInvocationTimeout)
                               .healthCheckType(healthCheckType)
                               .healthCheckHttpEndpoint(healthCheckHttpEndpoint)
                               .isSshEnabled(isSshEnabled)
                               .dockerInfo(dockerInfo)
                               .build();
    }

    private String getDefaultHealthCheckHttpEndpoint(String healthCheckType) {
        return HTTP_HEALTH_CHECK_TYPE.equals(healthCheckType) ? DEFAULT_HEALTH_CHECK_HTTP_ENDPOINT : null;
    }

}
