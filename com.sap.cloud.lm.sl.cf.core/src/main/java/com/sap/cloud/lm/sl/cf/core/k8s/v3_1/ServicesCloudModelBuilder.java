package com.sap.cloud.lm.sl.cf.core.k8s.v3_1;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.k8s.ResourceTypes;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.mta.model.ParametersContainer;
import com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3_1.Module;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;

public class ServicesCloudModelBuilder {

    static final String SERVICE_NAME_SUFFIX = "-service";
    private static final String DEFAULT_SERVICE_TYPE = "NodePort";

    private final PropertiesAccessor propertiesAccessor;

    public ServicesCloudModelBuilder(PropertiesAccessor propertiesAccessor) {
        this.propertiesAccessor = propertiesAccessor;
    }

    // FIXME: Reduce code duplication with DeploymentsCloudModelBuilder.
    public List<Service> build(DeploymentDescriptor descriptor) {
        List<Service> result = new ArrayList<>();
        for (Module module : descriptor.getModules3_1()) {
            ListUtil.addNonNull(result, buildIfDeployment(module));
        }
        return result;
    }

    private Service buildIfDeployment(Module module) {
        if (!isDeployment(module)) {
            return null;
        }
        return build(module);
    }

    private boolean isDeployment(Module module) {
        Map<String, Object> moduleParameters = propertiesAccessor.getParameters((ParametersContainer) module);
        String type = (String) moduleParameters.getOrDefault(SupportedParameters.TYPE, ResourceTypes.DEPLOYMENT);
        return ResourceTypes.DEPLOYMENT.equals(type);
    }

    private Service build(Module module) {
        return new ServiceBuilder().withMetadata(buildMeta(module))
            .withSpec(buildSpec(module))
            .build();
    }

    private ObjectMeta buildMeta(Module module) {
        return new ObjectMetaBuilder().withName(module.getName() + SERVICE_NAME_SUFFIX)
            .build();
    }

    private ServiceSpec buildSpec(Module module) {
        // FIXME: Allow users to specify custom service types and ports.
        return new ServiceSpecBuilder().withType(DEFAULT_SERVICE_TYPE)
            .addToPorts(buildDefaultServicePort())
            .build();
    }

    private ServicePort buildDefaultServicePort() {
        return new ServicePortBuilder().withPort(DeploymentsCloudModelBuilder.DEFAULT_CONTAINER_PORT)
            .build();
    }

}
