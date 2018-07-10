package com.sap.cloud.lm.sl.cf.core.k8s.v3_1;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.k8s.ResourceTypes;
import com.sap.cloud.lm.sl.cf.core.k8s.model.ServiceBinding;
import com.sap.cloud.lm.sl.cf.core.k8s.model.ServiceBindingSpec;
import com.sap.cloud.lm.sl.cf.core.k8s.model.ServiceInstance;
import com.sap.cloud.lm.sl.cf.core.k8s.model.ServiceInstanceSpec;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.mta.model.ParametersContainer;
import com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3_1.Resource;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

public class ServiceInstanceFactory implements ResourceFactory {

    private static final String SERVICE_BINDING_NAME_SUFFIX = "-binding";

    private final PropertiesAccessor propertiesAccessor;

    public ServiceInstanceFactory(PropertiesAccessor propertiesAccessor) {
        this.propertiesAccessor = propertiesAccessor;
    }

    @Override
    public List<String> getSupportedResourceTypes() {
        return Arrays.asList(ResourceTypes.SERVICE_INSTANCE);
    }

    @Override
    public List<HasMetadata> createFrom(DeploymentDescriptor descriptor, Resource resource, Map<String, String> labels) {
        return Arrays.asList(buildServiceInstance(resource, labels), buildServiceBinding(resource, labels));
    }

    private ServiceInstance buildServiceInstance(Resource resource, Map<String, String> labels) {
        ServiceInstance serviceInstance = new ServiceInstance();
        serviceInstance.setMetadata(buildServiceInstanceMeta(resource, labels));
        serviceInstance.setSpec(buildServiceInstanceSpec(resource, labels));
        return serviceInstance;
    }

    private ObjectMeta buildServiceInstanceMeta(Resource resource, Map<String, String> labels) {
        return new ObjectMetaBuilder().withName(resource.getName())
            .withLabels(labels)
            .build();
    }

    private ServiceInstanceSpec buildServiceInstanceSpec(Resource resource, Map<String, String> labels) {
        ServiceInstanceSpec spec = new ServiceInstanceSpec();
        spec.setClusterServiceClassExternalName(getParameter(resource, SupportedParameters.SERVICE));
        spec.setClusterServicePlanExternalName(getParameter(resource, SupportedParameters.SERVICE_PLAN));
        return spec;
    }

    @SuppressWarnings("unchecked")
    private <T> T getParameter(Resource resource, String parameterName) {
        Map<String, Object> resourceParameters = propertiesAccessor.getParameters((ParametersContainer) resource);
        return (T) resourceParameters.get(parameterName);
    }

    private ServiceBinding buildServiceBinding(Resource resource, Map<String, String> labels) {
        ServiceBinding serviceBinding = new ServiceBinding();
        serviceBinding.setMetadata(buildServiceBindingMeta(resource, labels));
        serviceBinding.setSpec(buildServiceBindingSpec(resource));
        return serviceBinding;
    }

    private ObjectMeta buildServiceBindingMeta(Resource resource, Map<String, String> labels) {
        return new ObjectMetaBuilder().withName(resource.getName() + SERVICE_BINDING_NAME_SUFFIX)
            .withLabels(labels)
            .build();
    }

    private ServiceBindingSpec buildServiceBindingSpec(Resource resource) {
        ServiceBindingSpec spec = new ServiceBindingSpec();
        spec.setInstanceRef(new LocalObjectReference(resource.getName()));
        spec.setSecretName(resource.getName());
        return spec;
    }

}
