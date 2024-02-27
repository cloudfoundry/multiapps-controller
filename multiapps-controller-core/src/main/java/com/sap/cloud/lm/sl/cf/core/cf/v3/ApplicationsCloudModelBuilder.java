package com.sap.cloud.lm.sl.cf.core.cf.v3;

import static com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil.isActive;
import static com.sap.cloud.lm.sl.common.util.CommonUtil.cast;

import java.util.ArrayList;
import java.util.List;

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
import com.sap.cloud.lm.sl.mta.model.v3.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3.Module;
import com.sap.cloud.lm.sl.mta.model.v3.RequiredDependency;

public class ApplicationsCloudModelBuilder extends com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationsCloudModelBuilder {

    private static final int MTA_MAJOR_VERSION = 3;

    public ApplicationsCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, CloudModelConfiguration configuration,
                                         DeployedMta deployedMta, SystemParameters systemParameters,
                                         XsPlaceholderResolver xsPlaceholderResolver, String deployId) {
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
        return getApplicationServices(module, resourceAndType -> filterExistingServicesRule(resourceAndType)
            && onlyActiveServicesRule(resourceAndType));
    }

    @Override
    protected List<ServiceKeyToInject> getServicesKeysToInject(com.sap.cloud.lm.sl.mta.model.v2.Module module) {
        return getServicesKeysToInject((Module) module);
    }

    protected List<ServiceKeyToInject> getServicesKeysToInject(Module module) {
        List<ServiceKeyToInject> serviceKeysToInject = new ArrayList<>();
        for (RequiredDependency dependency : module.getRequiredDependencies3()) {
            ServiceKeyToInject serviceKey = getServiceKeyToInject(dependency);
            CollectionUtils.addIgnoreNull(serviceKeysToInject, serviceKey);
        }
        return serviceKeysToInject;
    }

    protected ServiceKeyToInject getServiceKeyToInject(RequiredDependency dependency) {
        com.sap.cloud.lm.sl.mta.model.v3.Resource resource = (com.sap.cloud.lm.sl.mta.model.v3.Resource) getResource(dependency.getName());
        if (resource != null && CloudModelBuilderUtil.isActive(resource)) {
            return super.getServiceKeyToInject(dependency);
        }
        return null;
    }

    private boolean onlyActiveServicesRule(ResourceAndResourceType resourceAndResourceType) {
        return isActive(resourceAndResourceType.getResource());
    }
}
