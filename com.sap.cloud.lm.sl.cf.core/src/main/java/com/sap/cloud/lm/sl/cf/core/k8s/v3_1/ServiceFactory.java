package com.sap.cloud.lm.sl.cf.core.k8s.v3_1;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.k8s.Labels;
import com.sap.cloud.lm.sl.cf.core.k8s.ResourceTypes;
import com.sap.cloud.lm.sl.mta.handlers.v3_1.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.ParametersContainer;
import com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3_1.Module;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;

public class ServiceFactory implements ResourceFactory {

    static final String SERVICE_NAME_SUFFIX = "-service";
    private static final String DEFAULT_SERVICE_TYPE = "NodePort";

    private final PropertiesAccessor propertiesAccessor;

    public ServiceFactory(PropertiesAccessor propertiesAccessor) {
        this.propertiesAccessor = propertiesAccessor;
    }

    @Override
    public List<String> getSupportedResourceTypes() {
        return Arrays.asList(ResourceTypes.DEPLOYMENT);
    }

    @Override
    public List<HasMetadata> createFrom(DeploymentDescriptor descriptor, Module module, Map<String, String> labels) {
        return Arrays.asList(new ServiceBuilder().withMetadata(buildMeta(module, labels))
            .withSpec(buildSpec(module, labels))
            .build());
    }

    private ObjectMeta buildMeta(Module module, Map<String, String> labels) {
        return new ObjectMetaBuilder().withName(module.getName() + SERVICE_NAME_SUFFIX)
            .withLabels(labels)
            .build();
    }

    private ServiceSpec buildSpec(Module module, Map<String, String> labels) {
        // FIXME: Allow users to specify custom service types and ports.
        return new ServiceSpecBuilder().withType(DEFAULT_SERVICE_TYPE)
            .addToPorts(buildServicePort(module))
            .withSelector(buildSelector(labels))
            .build();
    }

    private ServicePort buildServicePort(Module module) {
        return new ServicePortBuilder().withPort(getContainerPort(module))
            .withProtocol(DeploymentFactory.DEFAULT_PROTOCOL)
            .build();
    }

    private Integer getContainerPort(Module module) {
        Map<String, Object> moduleParameters = propertiesAccessor.getParameters((ParametersContainer) module);
        // TODO: Validate the type of the value before casting it:
        return (Integer) moduleParameters.getOrDefault(com.sap.cloud.lm.sl.cf.core.k8s.SupportedParameters.CONTAINER_PORT,
            DeploymentFactory.DEFAULT_CONTAINER_PORT);
    }

    private Map<String, String> buildSelector(Map<String, String> labels) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put(Labels.APP, labels.get(Labels.APP));
        return result;
    }

}
