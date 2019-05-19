package com.sap.cloud.lm.sl.cf.core.cf.v3;

import java.util.List;

import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceKeyToInject;
import com.sap.cloud.lm.sl.cf.core.cf.DeploymentMode;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ResourceAndResourceType;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Module;
import com.sap.cloud.lm.sl.mta.model.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.Resource;

public class ApplicationCloudModelBuilder extends com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationCloudModelBuilder {

    private static final int MTA_MAJOR_VERSION = 3;

    public ApplicationCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, boolean prettyPrinting, DeployedMta deployedMta,
        String deployId, UserMessageLogger stepLogger) {
        super(deploymentDescriptor, prettyPrinting, deployedMta, deployId, stepLogger);
    }

    @Override
    public DeploymentMode getDeploymentMode() {
        boolean parallelDeploymentsEnabled = (boolean) deploymentDescriptor.getParameters()
            .getOrDefault(SupportedParameters.ENABLE_PARALLEL_DEPLOYMENTS, false);
        return parallelDeploymentsEnabled ? DeploymentMode.PARALLEL : DeploymentMode.SEQUENTIAL;
    }

    @Override
    protected HandlerFactory createHandlerFactory() {
        return new HandlerFactory(MTA_MAJOR_VERSION);
    }

    @Override
    public List<String> getAllApplicationServices(Module module) {
        return getApplicationServices(module, this::onlyActiveServicesRule);
    }

    @Override
    protected List<String> getApplicationServices(Module module) {
        return getApplicationServices(module,
            resourceAndType -> filterExistingServicesRule(resourceAndType) && onlyActiveServicesRule(resourceAndType));
    }

    @Override
    protected ServiceKeyToInject getServiceKeyToInject(RequiredDependency dependency) {
        Resource resource = getResource(dependency.getName());
        if (resource != null && resource.isActive()) {
            return super.getServiceKeyToInject(dependency);
        }
        return null;
    }

    private boolean onlyActiveServicesRule(ResourceAndResourceType resourceAndResourceType) {
        return resourceAndResourceType.getResource()
            .isActive();
    }
}
