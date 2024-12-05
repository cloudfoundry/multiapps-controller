package org.cloudfoundry.multiapps.controller.core.parser;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.util.PropertiesUtil;

import com.sap.cloudfoundry.client.facade.domain.DockerInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableStaging;
import com.sap.cloudfoundry.client.facade.domain.LifecycleType;
import com.sap.cloudfoundry.client.facade.domain.Staging;

public class StagingParametersParser implements ParametersParser<Staging> {

    private static final String DEFAULT_HEALTH_CHECK_HTTP_ENDPOINT = "/";
    private static final String HTTP_HEALTH_CHECK_TYPE = "http";

    @Override
    public Staging parse(List<Map<String, Object>> parametersList) {
        String command = (String) PropertiesUtil.getPropertyValue(parametersList, SupportedParameters.COMMAND, null);
        List<String> buildpacks = PropertiesUtil.getPluralOrSingular(parametersList, SupportedParameters.BUILDPACKS,
                                                                     SupportedParameters.BUILDPACK);
        String stackName = (String) PropertiesUtil.getPropertyValue(parametersList, SupportedParameters.STACK, null);
        Integer healthCheckTimeout = (Integer) PropertiesUtil.getPropertyValue(parametersList, SupportedParameters.HEALTH_CHECK_TIMEOUT,
                                                                               null);
        Integer healthCheckInvocationTimeout = (Integer) PropertiesUtil.getPropertyValue(parametersList,
                                                                                         SupportedParameters.HEALTH_CHECK_INVOCATION_TIMEOUT,
                                                                                         null);
        String healthCheckType = (String) PropertiesUtil.getPropertyValue(parametersList, SupportedParameters.HEALTH_CHECK_TYPE, null);
        String healthCheckHttpEndpoint = (String) PropertiesUtil.getPropertyValue(parametersList,
                                                                                  SupportedParameters.HEALTH_CHECK_HTTP_ENDPOINT,
                                                                                  getDefaultHealthCheckHttpEndpoint(healthCheckType));
        String readinessHealthCheckType = (String) PropertiesUtil.getPropertyValue(parametersList,
                                                                                   SupportedParameters.READINESS_HEALTH_CHECK_TYPE, null);
        String readinessHealthCheckHttpEndpoint = (String) PropertiesUtil.getPropertyValue(parametersList,
                                                                                           SupportedParameters.READINESS_HEALTH_CHECK_HTTP_ENDPOINT,
                                                                                           getDefaultHealthCheckHttpEndpoint(readinessHealthCheckType));
        Integer readinessHealthCheckInvocationTimeout = (Integer) PropertiesUtil.getPropertyValue(parametersList,
                                                                                                  SupportedParameters.READINESS_HEALTH_CHECK_INVOCATION_TIMEOUT,
                                                                                                  null);
        Integer readinessHealthCheckInterval = (Integer) PropertiesUtil.getPropertyValue(parametersList,
                                                                                         SupportedParameters.READINESS_HEALTH_CHECK_INTERVAL,
                                                                                         null);
        Boolean isSshEnabled = (Boolean) PropertiesUtil.getPropertyValue(parametersList, SupportedParameters.ENABLE_SSH, null);
        DockerInfo dockerInfo = new DockerInfoParser().parse(parametersList);
        LifecycleType lifecycleType = parseLifecycleType(parametersList);

        validateLifecycleType(lifecycleType, buildpacks, dockerInfo);

        return ImmutableStaging.builder()
                               .command(command)
                               .buildpacks(buildpacks)
                               .stackName(stackName)
                               .healthCheckTimeout(healthCheckTimeout)
                               .invocationTimeout(healthCheckInvocationTimeout)
                               .healthCheckType(healthCheckType)
                               .healthCheckHttpEndpoint(healthCheckHttpEndpoint)
                               .readinessHealthCheckType(readinessHealthCheckType)
                               .readinessHealthCheckHttpEndpoint(readinessHealthCheckHttpEndpoint)
                               .readinessHealthCheckTimeout(readinessHealthCheckInvocationTimeout)
                               .readinessHealthCheckInterval(readinessHealthCheckInterval)
                               .isSshEnabled(isSshEnabled)
                               .dockerInfo(dockerInfo)
                               .lifecycleType(lifecycleType)
                               .build();
    }

    private LifecycleType parseLifecycleType(List<Map<String, Object>> parametersList) {
        String lifecycleValue = (String) PropertiesUtil.getPropertyValue(parametersList, SupportedParameters.LIFECYCLE, null);
        if (lifecycleValue == null) {
            return null;
        }
        try {
            return LifecycleType.valueOf(lifecycleValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new SLException("Unsupported lifecycle value: " + lifecycleValue);
        }
    }

    private void validateLifecycleType(LifecycleType lifecycleType, List<String> buildpacks, DockerInfo dockerInfo) {
        if (lifecycleType == LifecycleType.CNB && (buildpacks == null || buildpacks.isEmpty())) {
            throw new SLException("Buildpacks must be provided when lifecycle is set to 'cnb'.");
        }
        // Validate Docker-specific conditions
        if (lifecycleType == LifecycleType.DOCKER) {
            if (dockerInfo == null) {
                throw new SLException("Docker information must be provided when lifecycle is set to 'docker'.");
            }
            if (buildpacks != null && !buildpacks.isEmpty()) {
                throw new SLException("Buildpacks must not be provided when lifecycle is set to 'docker'.");
            }
        } else if (dockerInfo != null && lifecycleType != null) {
            throw new SLException("Docker information must not be provided when lifecycle is set to " + lifecycleType + "'.");
        }
    }

    private String getDefaultHealthCheckHttpEndpoint(String healthCheckType) {
        return HTTP_HEALTH_CHECK_TYPE.equals(healthCheckType) ? DEFAULT_HEALTH_CHECK_HTTP_ENDPOINT : null;
    }

}
