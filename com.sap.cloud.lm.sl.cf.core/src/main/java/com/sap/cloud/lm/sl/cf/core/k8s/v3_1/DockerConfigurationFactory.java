package com.sap.cloud.lm.sl.cf.core.k8s.v3_1;

import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.k8s.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.k8s.model.DockerConfiguration;
import com.sap.cloud.lm.sl.cf.core.k8s.model.DockerRegistryCredentials;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.ParametersContainer;
import com.sap.cloud.lm.sl.mta.model.v3_1.Resource;

public class DockerConfigurationFactory {

    private static final String PARAMETER_0_IS_MISSING_FROM_RESOURCE_1 = "Parameter \"{0}\" is missing from resource \"{1}\".";
    private static final String PARAMETER_0_FROM_RESOURCE_1_HAS_AN_INVALID_TYPE_EXPECTED_2_ACTUAL_3 = "Parameter \"{0}\" from resource \"{1}\" has an invalid type. Expected: {2}, Actual: {3}";

    private final PropertiesAccessor propertiesAccessor;

    public DockerConfigurationFactory(PropertiesAccessor propertiesAccessor) {
        this.propertiesAccessor = propertiesAccessor;
    }

    public DockerConfiguration build(Resource resource) {
        Map<String, Object> resourceParameters = propertiesAccessor.getParameters((ParametersContainer) resource);
        validateParameters(resource, resourceParameters);
        return buildDockerConfigurationFromParameters(resourceParameters);
    }

    private void validateParameters(Resource resource, Map<String, Object> parameters) {
        validateParameter(resource, parameters, SupportedParameters.ContainerImageCredentialsSchema.REGISTRY, String.class);
        validateParameter(resource, parameters, SupportedParameters.ContainerImageCredentialsSchema.USERNAME, String.class);
        validateParameter(resource, parameters, SupportedParameters.ContainerImageCredentialsSchema.PASSWORD, String.class);
    }

    private void validateParameter(Resource resource, Map<String, Object> parameters, String parameterName, Class<?> expectedType) {
        Object parameter = parameters.get(parameterName);
        if (parameter == null) {
            throw new ContentException(PARAMETER_0_IS_MISSING_FROM_RESOURCE_1, parameterName, resource.getName());
        }
        if (!expectedType.isInstance(parameter)) {
            Class<?> actualType = parameter.getClass();
            throw new ContentException(PARAMETER_0_FROM_RESOURCE_1_HAS_AN_INVALID_TYPE_EXPECTED_2_ACTUAL_3, parameterName,
                resource.getName(), expectedType.getSimpleName(), actualType.getSimpleName());
        }
    }

    private DockerConfiguration buildDockerConfigurationFromParameters(Map<String, Object> parameters) {
        String registry = (String) parameters.get(SupportedParameters.ContainerImageCredentialsSchema.REGISTRY);
        String username = (String) parameters.get(SupportedParameters.ContainerImageCredentialsSchema.USERNAME);
        String password = (String) parameters.get(SupportedParameters.ContainerImageCredentialsSchema.PASSWORD);
        DockerRegistryCredentials registryCredentials = new DockerRegistryCredentials(username, password);
        return new DockerConfiguration.Builder().addCredentialsForRepository(registry, registryCredentials)
            .build();
    }

}
