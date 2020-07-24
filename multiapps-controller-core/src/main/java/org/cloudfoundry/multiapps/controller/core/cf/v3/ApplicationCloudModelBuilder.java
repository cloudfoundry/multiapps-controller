package org.cloudfoundry.multiapps.controller.core.cf.v3;

import java.util.List;

import org.cloudfoundry.multiapps.controller.client.lib.domain.ServiceKeyToInject;
import org.cloudfoundry.multiapps.controller.core.cf.DeploymentMode;
import org.cloudfoundry.multiapps.controller.core.cf.HandlerFactory;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ResourceAndResourceType;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.model.Resource;

public class ApplicationCloudModelBuilder extends org.cloudfoundry.multiapps.controller.core.cf.v2.ApplicationCloudModelBuilder {

    private static final int MTA_MAJOR_VERSION = 3;

    public ApplicationCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, boolean prettyPrinting, DeployedMta deployedMta,
                                        String deployId, String namespace, UserMessageLogger stepLogger) {
        super(deploymentDescriptor, prettyPrinting, deployedMta, deployId, namespace, stepLogger);
    }

    @Override
    public DeploymentMode getDeploymentMode() {
        boolean parallelDeploymentsEnabled = (boolean) deploymentDescriptor.getParameters()
                                                                           .getOrDefault(SupportedParameters.ENABLE_PARALLEL_DEPLOYMENTS,
                                                                                         false);
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
        return getApplicationServices(module, resourceAndType -> filterExistingServicesRule(resourceAndType)
            && onlyActiveServicesRule(resourceAndType));
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
