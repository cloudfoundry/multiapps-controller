package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ApplicationsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.LoopStepMetadata;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("reconfigureAppEnvironmentStep")
public class ReconfigureAppEnvironmentStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReconfigureAppEnvironmentStep.class);

    public static final String DEPLOY_ID_PREFIX = "deploy-";

    public static StepMetadata getMetadata() {
        return LoopStepMetadata.builder().id("reconfigureAppEnvironmentTask").displayName("Reconfigure App Environments").description(
            "Reconfigures App Environments with live routing values").children(RestartAppStep.getMetadata()).countVariable(
                Constants.VAR_APPS_TO_RESTART_COUNT).build();
    }

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);

        try {
            info(context, Messages.UPDATING_APP_ENVIRONMENT, LOGGER);

            DeployedMta deployedMta = StepsUtil.getDeployedMta(context);

            Set<String> mtaArchiveModules = StepsUtil.getMtaArchiveModules(context);
            debug(context, format(Messages.MTA_ARCHIVE_MODULES, mtaArchiveModules), LOGGER);
            List<DeployedMtaModule> deployedModules = (deployedMta != null) ? deployedMta.getModules() : Collections.emptyList();
            Set<String> deployedModuleNames = CloudModelBuilderUtil.getDeployedModuleNames(deployedModules);
            debug(context, format(Messages.DEPLOYED_MODULES, deployedModuleNames), LOGGER);
            Set<String> mtaModulesNames = StepsUtil.getMtaModules(context);
            debug(context, format(Messages.MTA_MODULES, mtaModulesNames), LOGGER);

            List<CloudApplicationExtended> updatedApps = getApplicationsCloudModelBuilder(context).build(mtaArchiveModules, mtaModulesNames,
                deployedModuleNames);

            List<CloudApplicationExtended> targetApps = StepsUtil.getAppsToDeploy(context);

            List<String> changedAppNames = new ArrayList<>();
            copyAppEnvironments(updatedApps, targetApps, changedAppNames);
            debug(context, format(Messages.APPS_WITH_ENVS_TO_RECONFIGURE_0, secureSerializer.toJson(changedAppNames)), LOGGER);
            updateAppEnvironments(context, targetApps, changedAppNames);
            StepsUtil.setAppsToDeploy(context, updatedApps);
            StepsUtil.setAppsToRestart(context, changedAppNames);

            context.setVariable(Constants.VAR_APPS_TO_RESTART_COUNT, changedAppNames.size());
            context.setVariable(Constants.VAR_INDEX_VARIABLE_NAME, Constants.VAR_APPS_INDEX);
            context.setVariable(Constants.VAR_APPS_INDEX, 0);

            debug(context, Messages.APPLICATION_ENVIRONMENTS_RECONFIGURED, LOGGER);

            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, Messages.ERROR_RECONFIGURING_APPS_ENVIRONMENTS, e, LOGGER);
            throw e;
        }
    }

    protected ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DelegateExecution context) {
        return StepsUtil.getApplicationsCloudModelBuilder(context);
    }

    private void updateAppEnvironments(DelegateExecution context, List<CloudApplicationExtended> targetApps, List<String> changedAppNames) {
        CloudFoundryOperations client = getCloudFoundryClient(context, LOGGER);
        for (CloudApplicationExtended app : targetApps) {
            String appName = app.getName();
            if (!changedAppNames.contains(appName)) {
                continue;
            }
            client.updateApplicationEnv(appName, app.getEnv());
        }
    }

    private void copyAppEnvironments(List<CloudApplicationExtended> updatedApps, List<CloudApplicationExtended> targetApps,
        List<String> changedApps) {
        for (CloudApplicationExtended updatedApp : updatedApps) {
            final String appName = updatedApp.getName();
            CloudApplicationExtended targetApp = targetApps.stream().filter((app) -> app.getName().equals(appName)).findFirst().get();
            Map<String, String> newEnvironment = updatedApp.getEnvAsMap();
            if (!newEnvironment.equals(targetApp.getEnvAsMap())) {
                Map<Object, Object> newEnv = new HashMap<Object, Object>();
                newEnvironment.forEach((key, value) -> newEnv.put(key, value));
                targetApp.setEnv(newEnv);
                changedApps.add(appName);
            }
        }
    }
}
