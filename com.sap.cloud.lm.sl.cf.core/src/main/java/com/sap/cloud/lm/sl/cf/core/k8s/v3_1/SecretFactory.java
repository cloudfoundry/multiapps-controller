package com.sap.cloud.lm.sl.cf.core.k8s.v3_1;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.k8s.ResourceTypes;
import com.sap.cloud.lm.sl.cf.core.k8s.SupportedParameters;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.ParametersContainer;
import com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3_1.Resource;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;

public class SecretFactory implements ResourceFactory {

    private static final String PARAMETER_0_IS_MISSING_FROM_RESOURCE_1 = "Parameter \"{0}\" is missing from resource \"{1}\".";
    private static final String PARAMETER_0_FROM_RESOURCE_1_HAS_AN_INVALID_TYPE_EXPECTED_2_ACTUAL_3 = "Parameter \"{0}\" from resource \"{1}\" has an invalid type. Expected: {2}, Actual: {3}";

    private final PropertiesAccessor propertiesAccessor;

    public SecretFactory(PropertiesAccessor propertiesAccessor) {
        this.propertiesAccessor = propertiesAccessor;
    }

    @Override
    public List<String> getSupportedResourceTypes() {
        return Arrays.asList(ResourceTypes.SECRET);
    }

    @Override
    public List<HasMetadata> createFrom(DeploymentDescriptor descriptor, Resource resource, Map<String, String> labels) {
        return Arrays.asList(buildSecret(resource, labels));
    }

    private Secret buildSecret(Resource resource, Map<String, String> labels) {
        return new SecretBuilder().withMetadata(buildMeta(resource, labels))
            .withData(buildData(resource))
            .build();
    }

    private ObjectMeta buildMeta(Resource resource, Map<String, String> labels) {
        return new ObjectMetaBuilder().withName(resource.getName())
            .withLabels(labels)
            .build();
    }

    private Map<String, String> buildData(Resource resource) {
        Map<String, Object> resourceParameters = propertiesAccessor.getParameters((ParametersContainer) resource);
        return getData(resource, resourceParameters);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getData(Resource resource, Map<String, Object> resourceParameters) {
        // TODO: Validate the data and flatten it if it contains values different than strings:
        validateParameter(resource, resourceParameters, SupportedParameters.DATA, Map.class);
        Map<Object, Object> data = (Map<Object, Object>) resourceParameters.get(SupportedParameters.DATA);
        Map<String, String> flattenedData = flatten(data);
        Map<String, String> encodedData = encode(flattenedData);
        return encodedData;
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

    private Map<String, String> flatten(Map<Object, Object> data) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            result.put(flattenValue(entry.getKey()), flattenValue(entry.getValue()));
        }
        return result;
    }

    private String flattenValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return JsonUtil.toJson(value, true);
    }

    private Map<String, String> encode(Map<String, String> data) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            result.put(entry.getKey(), encodeValue(entry.getValue()));
        }
        return result;
    }

    private String encodeValue(String value) {
        byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);
        byte[] encodedBytes = Base64.getEncoder()
            .encode(valueBytes);
        return new String(encodedBytes, StandardCharsets.UTF_8);
    }

}
