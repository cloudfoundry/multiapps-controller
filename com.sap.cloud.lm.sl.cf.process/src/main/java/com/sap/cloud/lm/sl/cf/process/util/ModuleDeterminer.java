package com.sap.cloud.lm.sl.cf.process.util;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.immutables.value.Value;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.EnvMtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.MtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.util.MtaMetadataUtil;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.process.steps.ProcessContext;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Module;

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
