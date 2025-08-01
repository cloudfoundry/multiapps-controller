package org.cloudfoundry.multiapps.controller.core.parser;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import com.sap.cloudfoundry.client.facade.domain.DockerInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableStaging;
import com.sap.cloudfoundry.client.facade.domain.LifecycleType;
import com.sap.cloudfoundry.client.facade.domain.Staging;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.util.PropertiesUtil;
import org.springframework.util.CollectionUtils;

import static org.cloudfoundry.multiapps.controller.core.Messages.BUILDPACKS_NOT_ALLOWED_WITH_DOCKER;
import static org.cloudfoundry.multiapps.controller.core.Messages.BUILDPACKS_REQUIRED_FOR_CNB;
import static org.cloudfoundry.multiapps.controller.core.Messages.DOCKER_INFO_NOT_ALLOWED_WITH_LIFECYCLE;
import static org.cloudfoundry.multiapps.controller.core.Messages.DOCKER_INFO_REQUIRED;
import static org.cloudfoundry.multiapps.controller.core.Messages.UNSUPPORTED_LIFECYCLE_VALUE;

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
            throw new ContentException(MessageFormat.format(UNSUPPORTED_LIFECYCLE_VALUE, lifecycleValue));
        }
    }

    private void validateLifecycleType(LifecycleType lifecycleType, List<String> buildpacks, DockerInfo dockerInfo) {
        validateBuildpacksWithCNB(lifecycleType, buildpacks);

        // Validate Docker-specific conditions
        validateDockerInfoWithDocker(lifecycleType, dockerInfo);
        validateBuildpacksWithDocker(lifecycleType, buildpacks);
        validateDockerInfoWithNonDocker(lifecycleType, dockerInfo);
    }

    private void validateBuildpacksWithCNB(LifecycleType lifecycleType, List<String> buildpacks) {
        if (lifecycleType == LifecycleType.CNB && CollectionUtils.isEmpty(buildpacks)) {
            throw new ContentException(BUILDPACKS_REQUIRED_FOR_CNB);
        }
    }

    private void validateDockerInfoWithDocker(LifecycleType lifecycleType, DockerInfo dockerInfo) {
        if (lifecycleType == LifecycleType.DOCKER && dockerInfo == null) {
            throw new ContentException(DOCKER_INFO_REQUIRED);
        }
    }

    private void validateBuildpacksWithDocker(LifecycleType lifecycleType, List<String> buildpacks) {
        if (lifecycleType == LifecycleType.DOCKER && !CollectionUtils.isEmpty(buildpacks)) {
            throw new ContentException(BUILDPACKS_NOT_ALLOWED_WITH_DOCKER);
        }
    }

    private void validateDockerInfoWithNonDocker(LifecycleType lifecycleType, DockerInfo dockerInfo) {
        if (lifecycleType != LifecycleType.DOCKER && lifecycleType != null && dockerInfo != null) {
            throw new ContentException(
                MessageFormat.format(DOCKER_INFO_NOT_ALLOWED_WITH_LIFECYCLE, lifecycleType));
        }
    }

    private String getDefaultHealthCheckHttpEndpoint(String healthCheckType) {
        return HTTP_HEALTH_CHECK_TYPE.equals(healthCheckType) ? DEFAULT_HEALTH_CHECK_HTTP_ENDPOINT : null;
    }

}
