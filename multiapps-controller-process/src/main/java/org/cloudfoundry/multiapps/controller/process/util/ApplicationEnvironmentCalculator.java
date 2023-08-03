package org.cloudfoundry.multiapps.controller.process.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudOperationException;

@Named
public class ApplicationEnvironmentCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationEnvironmentCalculator.class);

    private final DeploymentTypeDeterminer deploymentTypeDeterminer;

    @Inject
    public ApplicationEnvironmentCalculator(DeploymentTypeDeterminer deploymentTypeDeterminer) {
        this.deploymentTypeDeterminer = deploymentTypeDeterminer;
    }

    public Map<String, String> calculateNewApplicationEnv(ProcessContext context, CloudApplicationExtended newApp) {
        if (!(newApp.getAttributesUpdateStrategy()
                    .shouldKeepExistingEnv()
            && deploymentTypeDeterminer.determineDeploymentType(context)
                                       .equals(ProcessType.BLUE_GREEN_DEPLOY))) {
            return newApp.getEnv();
        }
        if (context.getVariable(Variables.DEPLOYED_MTA) == null) {
            context.getStepLogger()
                   .debug(Messages.INITIAL_DEPLOYMENT_WILL_NOT_SEARCH_FOR_LIVE_APPLICATION);
            return newApp.getEnv();
        }
        context.getStepLogger()
               .debug(Messages.DETECTING_LIVE_APPLICATION_ENV);
        return MapUtil.merge(getLiveApplicationEnvIfExistsOrReturnEmpty(context), newApp.getEnv());
    }

    private Map<String, String> getLiveApplicationEnvIfExistsOrReturnEmpty(ProcessContext context) {
        Module moduleToDeploy = context.getVariable(Variables.MODULE_TO_DEPLOY);
        List<DeployedMtaApplication> deployedApplications = context.getVariable(Variables.DEPLOYED_MTA)
                                                                   .getApplications();
        Optional<DeployedMtaApplication> deployedMtaApplication = getDeployedLiveApplication(deployedApplications, moduleToDeploy);
        if (deployedMtaApplication.isEmpty()) {
            context.getStepLogger()
                   .debug(Messages.MODULE_0_WAS_NOT_FOUND, moduleToDeploy.getName());
            return Collections.emptyMap();
        }
        return getApplicationEnv(context, deployedMtaApplication.get()
                                                                .getMetadata()
                                                                .getGuid());
    }

    private Optional<DeployedMtaApplication> getDeployedLiveApplication(List<DeployedMtaApplication> deployedApplications,
                                                                        Module moduleToDeploy) {
        return deployedApplications.stream()
                                   .filter(app -> app.getModuleName()
                                                     .equals(moduleToDeploy.getName()))
                                   .filter(app -> app.getProductizationState() == DeployedMtaApplication.ProductizationState.LIVE)
                                   .findFirst();
    }

    private Map<String, String> getApplicationEnv(ProcessContext context, UUID liveAppGuid) {
        try {
            return context.getControllerClient()
                          .getApplicationEnvironment(liveAppGuid);
        } catch (CloudOperationException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                context.getStepLogger()
                       .error(Messages.APPLICATION_0_WAS_NOT_FOUND, liveAppGuid);
                LOGGER.error(e.getMessage(), e);
                return Collections.emptyMap();
            }
            throw e;
        }
    }

}
