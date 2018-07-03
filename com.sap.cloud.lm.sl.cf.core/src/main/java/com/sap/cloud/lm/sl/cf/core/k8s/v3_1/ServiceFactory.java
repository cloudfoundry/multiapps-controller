package com.sap.cloud.lm.sl.cf.core.k8s.v3_1;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.k8s.ResourceTypes;
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

    @Override
    public List<String> getSupportedResourceTypes() {
        return Arrays.asList(ResourceTypes.DEPLOYMENT);
    }

    @Override
    public List<HasMetadata> createFrom(DeploymentDescriptor descriptor, Module module, Map<String, String> labels) {
        return Arrays.asList(new ServiceBuilder().withMetadata(buildMeta(module, labels))
            .withSpec(buildSpec(module))
            .build());
    }

    private ObjectMeta buildMeta(Module module, Map<String, String> labels) {
        return new ObjectMetaBuilder().withName(module.getName() + SERVICE_NAME_SUFFIX)
            .withLabels(labels)
            .build();
    }

    private ServiceSpec buildSpec(Module module) {
        // FIXME: Allow users to specify custom service types and ports.
        return new ServiceSpecBuilder().withType(DEFAULT_SERVICE_TYPE)
            .addToPorts(buildDefaultServicePort())
            .build();
    }

    private ServicePort buildDefaultServicePort() {
        return new ServicePortBuilder().withPort(DeploymentFactory.DEFAULT_CONTAINER_PORT)
            .build();
    }

}
