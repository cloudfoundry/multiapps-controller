package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.HandlerFactory;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.EnvMtaMetadataParser;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.MtaMetadataParser;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.util.MtaMetadataUtil;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.steps.StepsUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.immutables.value.Value;

@Value.Immutable
public abstract class ModuleDeterminer {

    public Module determineModuleToDeploy(ProcessContext context) {
        Module moduleToDeploy = context.getVariable(Variables.MODULE_TO_DEPLOY);
        return moduleToDeploy != null ? moduleToDeploy : determineModule(context);
    }

    private Module determineModule(ProcessContext context) {
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);
        if (deploymentDescriptor == null) {
            // This will be the case only when the process is undeploy.
            return null;
        }
        return determineModuleFromDeploymentDescriptor(context.getVariable(Variables.APP_TO_PROCESS), deploymentDescriptor);
    }

    private Module determineModuleFromDeploymentDescriptor(CloudApplicationExtended cloudApplication,
                                                           DeploymentDescriptor deploymentDescriptor) {
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(getContext().getExecution());
        String moduleName = computeModuleName(cloudApplication);
        return findModuleByNameFromDeploymentDescriptor(handlerFactory, deploymentDescriptor, moduleName);
    }

    private String computeModuleName(CloudApplicationExtended cloudApplication) {
        if (cloudApplication.getModuleName() != null) {
            return cloudApplication.getModuleName();
        }
        // This case handles the deletion of old applications when the process is blue-green deployment. Here the application is taken
        // from the
        // CloudController and thus we do not have moduleName in it.
        return calculateModuleName(cloudApplication);
    }

    private String calculateModuleName(CloudApplicationExtended cloudApplication) {
        return getDeployedMtaApplication(cloudApplication).getModuleName();
    }

    private DeployedMtaApplication getDeployedMtaApplication(CloudApplication app) {
        if (!MtaMetadataUtil.hasMtaMetadata(app)) {
            return getEnvMtaMetadataParser().parseDeployedMtaApplication(app);
        }
        return getMtaMetadataParser().parseDeployedMtaApplication(app);
    }

    private Module findModuleByNameFromDeploymentDescriptor(HandlerFactory handlerFactory, DeploymentDescriptor deploymentDescriptor,
                                                            String moduleName) {
        return handlerFactory.getDescriptorHandler()
                             .findModule(deploymentDescriptor, moduleName);
    }

    public abstract MtaMetadataParser getMtaMetadataParser();

    public abstract EnvMtaMetadataParser getEnvMtaMetadataParser();

    public abstract ProcessContext getContext();

}
