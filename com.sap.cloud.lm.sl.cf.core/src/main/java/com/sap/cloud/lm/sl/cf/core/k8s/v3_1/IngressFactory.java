package com.sap.cloud.lm.sl.cf.core.k8s.v3_1;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.k8s.ResourceTypes;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.ParametersContainer;
import com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3_1.Module;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPathBuilder;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressRuleValue;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressRuleValueBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressBackend;
import io.fabric8.kubernetes.api.model.extensions.IngressBackendBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressRule;
import io.fabric8.kubernetes.api.model.extensions.IngressRuleBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressSpec;
import io.fabric8.kubernetes.api.model.extensions.IngressSpecBuilder;

public class IngressFactory implements ResourceFactory {

    private static final String PARAMETER_0_FROM_MODULE_1_HAS_AN_INVALID_TYPE_EXPECTED_2_ACTUAL_3 = "Parameter \"{0}\" from module \"{1}\" has an invalid type. Expected: {2}, Actual: {3}";

    private static final String INGRESS_NAME_SUFFIX = "-ingress";

    private final PropertiesAccessor propertiesAccessor;

    public IngressFactory(PropertiesAccessor propertiesAccessor) {
        this.propertiesAccessor = propertiesAccessor;
    }

    @Override
    public List<String> getSupportedResourceTypes() {
        return Arrays.asList(ResourceTypes.DEPLOYMENT);
    }

    @Override
    public List<HasMetadata> createFrom(DeploymentDescriptor descriptor, Module module, Map<String, String> labels) {
        String route = getRoute(module);
        if (route == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(new IngressBuilder().withMetadata(buildMeta(module, labels))
            .withSpec(buildSpec(module, route))
            .build());
    }

    private String getRoute(Module module) {
        Map<String, Object> moduleParameters = propertiesAccessor.getParameters((ParametersContainer) module);
        Object route = moduleParameters.get(com.sap.cloud.lm.sl.cf.core.k8s.SupportedParameters.ROUTE);
        if (route == null) {
            return null;
        }
        if (!(route instanceof String)) {
            Class<?> actualType = route.getClass();
            throw new ContentException(PARAMETER_0_FROM_MODULE_1_HAS_AN_INVALID_TYPE_EXPECTED_2_ACTUAL_3,
                com.sap.cloud.lm.sl.cf.core.k8s.SupportedParameters.ROUTE, module.getName(), String.class.getSimpleName(),
                actualType.getSimpleName());
        }
        return (String) route;
    }

    private ObjectMeta buildMeta(Module module, Map<String, String> labels) {
        return new ObjectMetaBuilder().withName(module.getName() + INGRESS_NAME_SUFFIX)
            .withLabels(labels)
            .build();
    }

    private IngressSpec buildSpec(Module module, String route) {
        return new IngressSpecBuilder().addToRules(buildRule(module, route))
            .build();
    }

    private IngressRule buildRule(Module module, String route) {
        return new IngressRuleBuilder().withHost(route)
            .withHttp(buildHttpRule(module))
            .build();
    }

    private HTTPIngressRuleValue buildHttpRule(Module module) {
        return new HTTPIngressRuleValueBuilder().addToPaths(buildHttpRulePath(module))
            .build();
    }

    private HTTPIngressPath buildHttpRulePath(Module module) {
        return new HTTPIngressPathBuilder().withBackend(buildBackend(module))
            .build();
    }

    private IngressBackend buildBackend(Module module) {
        return new IngressBackendBuilder().withServiceName(module.getName() + ServiceFactory.SERVICE_NAME_SUFFIX)
            .withServicePort(new IntOrString(DeploymentFactory.DEFAULT_CONTAINER_PORT))
            .build();
    }

}
