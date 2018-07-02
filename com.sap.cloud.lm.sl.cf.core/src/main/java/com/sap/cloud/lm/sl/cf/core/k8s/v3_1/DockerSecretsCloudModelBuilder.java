package com.sap.cloud.lm.sl.cf.core.k8s.v3_1;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.k8s.ResourceTypes;
import com.sap.cloud.lm.sl.cf.core.k8s.model.DockerConfiguration;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.mta.model.ParametersContainer;
import com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3_1.Resource;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;

public class DockerSecretsCloudModelBuilder {

    private static final String DOCKER_IMAGE_SECRET_TYPE = "kubernetes.io/dockerconfigjson";
    private static final String DOCKER_IMAGE_SECRET_KEY = ".dockerconfigjson";

    private final PropertiesAccessor propertiesAccessor;

    public DockerSecretsCloudModelBuilder(PropertiesAccessor propertiesAccessor) {
        this.propertiesAccessor = propertiesAccessor;
    }

    public List<Secret> build(DeploymentDescriptor descriptor) {
        List<Secret> result = new ArrayList<>();
        for (Resource resource : descriptor.getResources3_1()) {
            ListUtil.addNonNull(result, buildIfDockerSecret(resource));
        }
        return result;
    }

    private Secret buildIfDockerSecret(Resource resource) {
        if (!isDockerSecret(resource)) {
            return null;
        }
        DockerConfiguration dockerConfiguration = buildDockerConfiguration(resource);
        return build(resource, dockerConfiguration);
    }

    private boolean isDockerSecret(Resource resource) {
        Map<String, Object> resourceParameters = propertiesAccessor.getParameters((ParametersContainer) resource);
        return ResourceTypes.DOCKER_SECRET.equals(resourceParameters.get(SupportedParameters.TYPE));
    }

    private DockerConfiguration buildDockerConfiguration(Resource resource) {
        return new DockerConfigurationCloudModelBuilder(propertiesAccessor).build(resource);
    }

    private Secret build(Resource resource, DockerConfiguration dockerConfiguration) {
        return new SecretBuilder().withMetadata(buildSecretMeta(resource))
            .withType(DOCKER_IMAGE_SECRET_TYPE)
            .withData(buildSecretData(dockerConfiguration))
            .build();
    }

    private ObjectMeta buildSecretMeta(Resource resource) {
        return new ObjectMetaBuilder().withName(resource.getName())
            .build();
    }

    private Map<String, String> buildSecretData(DockerConfiguration dockerConfiguration) {
        Map<String, String> data = new HashMap<>();
        data.put(DOCKER_IMAGE_SECRET_KEY, getBase64EncodedDockerConfiguration(dockerConfiguration));
        return data;
    }

    private String getBase64EncodedDockerConfiguration(DockerConfiguration dockerConfiguration) {
        String dockerConfigurationJson = JsonUtil.toJson(dockerConfiguration, true);
        byte[] encodedDockerConfiguration = Base64.getEncoder()
            .encode(dockerConfigurationJson.getBytes(StandardCharsets.UTF_8));
        return new String(encodedDockerConfiguration, StandardCharsets.UTF_8);
    }

}
