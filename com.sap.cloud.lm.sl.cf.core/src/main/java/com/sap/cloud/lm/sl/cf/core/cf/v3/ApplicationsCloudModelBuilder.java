package com.sap.cloud.lm.sl.cf.core.cf.v3;

import static com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil.isActive;
import static com.sap.cloud.lm.sl.common.util.CommonUtil.cast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceKeyToInject;
import com.sap.cloud.lm.sl.cf.core.cf.DeploymentMode;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.v2.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ResourceAndResourceType;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;
import com.sap.cloud.lm.sl.mta.model.v3.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3.Module;
import com.sap.cloud.lm.sl.mta.model.v3.RequiredDependency;
import com.sap.cloud.lm.sl.mta.util.PropertiesUtil;

public class ApplicationsCloudModelBuilder extends com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationsCloudModelBuilder {

    private static final int MTA_MAJOR_VERSION = 3;

    public ApplicationsCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, CloudModelConfiguration configuration,
        DeployedMta deployedMta, SystemParameters systemParameters, XsPlaceholderResolver xsPlaceholderResolver, String deployId) {
        super(deploymentDescriptor, configuration, deployedMta, systemParameters, xsPlaceholderResolver, deployId);
    }

    @Override
    public DeploymentMode getDeploymentMode() {
        DeploymentDescriptor descriptorV3 = cast(deploymentDescriptor);
        boolean parallelDeploymentsEnabled = (Boolean) descriptorV3.getParameters()
            .getOrDefault(SupportedParameters.ENABLE_PARALLEL_DEPLOYMENTS, false);
        return parallelDeploymentsEnabled ? DeploymentMode.PARALLEL : DeploymentMode.SEQUENTIAL;
    }

    @Override
    protected HandlerFactory createHandlerFactory() {
        return new HandlerFactory(MTA_MAJOR_VERSION);
    }

    @Override
    protected List<String> getAllApplicationServices(com.sap.cloud.lm.sl.mta.model.v2.Module module) {
        return getApplicationServices((Module) module, this::onlyActiveServicesRule);
    }

    @Override
    protected List<String> getApplicationServices(com.sap.cloud.lm.sl.mta.model.v2.Module module) {
        return getApplicationServices(module,
            resourceAndType -> filterExistingServicesRule(resourceAndType) && onlyActiveServicesRule(resourceAndType));
    }

    @Override
    protected List<ServiceKeyToInject> getServicesKeysToInject(com.sap.cloud.lm.sl.mta.model.v2.Module module) {
        return getServicesKeysToInject((Module) module);
    }

    protected List<ServiceKeyToInject> getServicesKeysToInject(Module module) {
        List<ServiceKeyToInject> serviceKeysToInject = new ArrayList<>();
        for (RequiredDependency dependency : module.getRequiredDependencies3()) {
            ServiceKeyToInject serviceKey = getServiceKeyToInject(dependency);
            if (isActiveServiceKey(serviceKey)) {
                CollectionUtils.addIgnoreNull(serviceKeysToInject, serviceKey);
            }
        }
        return serviceKeysToInject;
    }

    private boolean isActiveServiceKey(ServiceKeyToInject serviceKeyToInject) {
        if (serviceKeyToInject != null) {
            Resource resource = getResource(serviceKeyToInject.getServiceName());
            if (resource != null) {
                return isActive(resource);
            }
        }
        return false;
    }

    protected ServiceKeyToInject getServiceKeyToInject(RequiredDependency dependency) {
        com.sap.cloud.lm.sl.mta.model.v3.Resource resource = (com.sap.cloud.lm.sl.mta.model.v3.Resource) getResource(dependency.getName());
        if (resource != null && CloudModelBuilderUtil.isServiceKey(resource, propertiesAccessor)
            && CloudModelBuilderUtil.isActive(resource)) {
            Map<String, Object> resourceParameters = propertiesAccessor.getParameters(resource);
            String serviceName = PropertiesUtil.getRequiredParameter(resourceParameters, SupportedParameters.SERVICE_NAME);
            String serviceKeyName = (String) resourceParameters.getOrDefault(SupportedParameters.SERVICE_KEY_NAME, resource.getName());
            String envVarName = (String) dependency.getParameters()
                .getOrDefault(SupportedParameters.ENV_VAR_NAME, serviceKeyName);
            return new ServiceKeyToInject(envVarName, serviceName, serviceKeyName);
        }
        return null;
    }

    private boolean onlyActiveServicesRule(ResourceAndResourceType resourceAndResourceType) {
        return isActive(resourceAndResourceType.getResource());
    }
}
