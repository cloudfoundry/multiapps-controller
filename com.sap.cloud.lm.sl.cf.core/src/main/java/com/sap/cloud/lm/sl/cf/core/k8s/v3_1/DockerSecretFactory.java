package com.sap.cloud.lm.sl.cf.core.k8s.v3_1;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.k8s.ResourceTypes;
import com.sap.cloud.lm.sl.cf.core.k8s.model.DockerConfiguration;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3_1.Resource;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;

public class DockerSecretFactory implements ResourceFactory {

    private static final String DOCKER_CONFIGURATION_FILE_AUTHENTICATION_KEY = "auths";
    
    private static final String DOCKER_IMAGE_SECRET_TYPE = "kubernetes.io/dockerconfigjson";
    private static final String DOCKER_IMAGE_SECRET_KEY = ".dockerconfigjson";

    private final PropertiesAccessor propertiesAccessor;

    public DockerSecretFactory(PropertiesAccessor propertiesAccessor) {
        this.propertiesAccessor = propertiesAccessor;
    }

    @Override
    public List<String> getSupportedResourceTypes() {
        return Arrays.asList(ResourceTypes.DOCKER_SECRET);
    }

    @Override
    public List<HasMetadata> createFrom(DeploymentDescriptor descriptor, Resource resource, Map<String, String> labels) {
        DockerConfiguration dockerConfiguration = buildDockerConfiguration(resource);
        return Arrays.asList(build(resource, dockerConfiguration, labels));
    }

    private DockerConfiguration buildDockerConfiguration(Resource resource) {
        return new DockerConfigurationFactory(propertiesAccessor).build(resource);
    }

    private Secret build(Resource resource, DockerConfiguration dockerConfiguration, Map<String, String> labels) {
        return new SecretBuilder().withMetadata(buildSecretMeta(resource, labels))
            .withType(DOCKER_IMAGE_SECRET_TYPE)
            .withData(buildSecretData(dockerConfiguration))
            .build();
    }

    private ObjectMeta buildSecretMeta(Resource resource, Map<String, String> labels) {
        return new ObjectMetaBuilder().withName(resource.getName())
            .withLabels(labels)
            .build();
    }

    private Map<String, String> buildSecretData(DockerConfiguration dockerConfiguration) {
        Map<String, String> data = new HashMap<>();
        data.put(DOCKER_IMAGE_SECRET_KEY, getBase64EncodedDockerConfiguration(dockerConfiguration));
        return data;
    }

    private String getBase64EncodedDockerConfiguration(DockerConfiguration dockerConfiguration) {
        String dockerConfigurationJson = JsonUtil.toJson(dockerConfiguration.toMap(), true);
        Base64.Encoder encoder = Base64.getEncoder();
        byte[] encodedDockerConfiguration = encoder.encode(dockerConfigurationJson.getBytes(StandardCharsets.UTF_8));
        return new String(encodedDockerConfiguration, StandardCharsets.UTF_8);
    }

}
