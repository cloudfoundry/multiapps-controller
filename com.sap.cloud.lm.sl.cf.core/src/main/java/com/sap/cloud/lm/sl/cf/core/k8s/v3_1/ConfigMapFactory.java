package com.sap.cloud.lm.sl.cf.core.k8s.v3_1;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.cf.v1_0.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.cf.v2_0.ApplicationEnvironmentCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.k8s.ResourceTypes;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.mta.handlers.v3_1.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3_1.Module;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

public class ConfigMapFactory implements ResourceFactory {

    private static final String CONFIG_MAPS_NAME_SUFFIX = "-config";

    private final DescriptorHandler descriptorHandler;
    private final PropertiesAccessor propertiesAccessor;

    public ConfigMapFactory(DescriptorHandler handler, PropertiesAccessor propertiesAccessor) {
        this.descriptorHandler = handler;
        this.propertiesAccessor = propertiesAccessor;
    }

    @Override
    public List<String> getSupportedResourceTypes() {
        return Arrays.asList(ResourceTypes.DEPLOYMENT, ResourceTypes.JOB);
    }

    @Override
    public List<HasMetadata> createFrom(DeploymentDescriptor descriptor, Module module, Map<String, String> labels) {
        Map<Object, Object> data = buildData(descriptor, module);
        if (data.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(new ConfigMapBuilder().withMetadata(buildMeta(module, labels))
            .withData(MapUtil.cast(data))
            .build());
    }

    private ObjectMeta buildMeta(Module module, Map<String, String> labels) {
        return new ObjectMetaBuilder().withName(module.getName() + CONFIG_MAPS_NAME_SUFFIX)
            .withLabels(labels)
            .build();
    }

    private Map<Object, Object> buildData(DeploymentDescriptor descriptor, Module module) {
        // FIXME: Do not use classes from the "cf" package. Extract the common code somewhere else.
        ApplicationEnvironmentCloudModelBuilder envBuilder = createEnvBuilder(descriptor);
        return envBuilder.buildWithoutMetadata(module);
    }

    private ApplicationEnvironmentCloudModelBuilder createEnvBuilder(DeploymentDescriptor descriptor) {
        CloudModelConfiguration configuration = createBuilderConfiguration();
        return new ApplicationEnvironmentCloudModelBuilder(configuration, descriptor, new XsPlaceholderResolver(), descriptorHandler,
            propertiesAccessor, null);
    }

    private CloudModelConfiguration createBuilderConfiguration() {
        CloudModelConfiguration configuration = new CloudModelConfiguration();
        configuration.setPrettyPrinting(true);
        return configuration;
    }

}
