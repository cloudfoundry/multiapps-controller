package com.sap.cloud.lm.sl.cf.core.k8s.v3_1;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.cf.v1_0.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.cf.v2_0.ApplicationEnvironmentCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.mta.handlers.v3_1.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3_1.Module;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

public class ConfigMapsCloudModelBuilder {

    public List<ConfigMap> build(DeploymentDescriptor descriptor) {
        List<ConfigMap> result = new ArrayList<>();
        for (Module module : descriptor.getModules3_1()) {
            ListUtil.addNonNull(result, build(descriptor, module));
        }
        return result;
    }

    private ConfigMap build(DeploymentDescriptor descriptor, Module module) {
        Map<Object, Object> data = buildConfigMapData(descriptor, module);
        if (data.isEmpty()) {
            return null;
        }
        ConfigMap configMap = new ConfigMap();
        configMap.setMetadata(new ObjectMetaBuilder().withName(module.getName() + "-config")
            .build());
        configMap.setData(MapUtil.cast(data));
        return configMap;
    }

    private Map<Object, Object> buildConfigMapData(DeploymentDescriptor descriptor, Module module) {
        // FIXME: Do not use classes from the "cf" package. Extract the common code somewhere else.
        ApplicationEnvironmentCloudModelBuilder envBuilder = createEnvBuilder(descriptor);
        return envBuilder.buildWithoutMetadata(module);
    }

    private ApplicationEnvironmentCloudModelBuilder createEnvBuilder(DeploymentDescriptor descriptor) {
        CloudModelConfiguration configuration = createBuilderConfiguration();
        return new ApplicationEnvironmentCloudModelBuilder(configuration, descriptor, new XsPlaceholderResolver(),
            createDescriptorHandler(), createPropertiesAccessor(), null);
    }

    private CloudModelConfiguration createBuilderConfiguration() {
        CloudModelConfiguration configuration = new CloudModelConfiguration();
        configuration.setPrettyPrinting(true);
        return configuration;
    }

    protected DescriptorHandler createDescriptorHandler() {
        return new DescriptorHandler();
    }

    protected PropertiesAccessor createPropertiesAccessor() {
        return new PropertiesAccessor();
    }

}
