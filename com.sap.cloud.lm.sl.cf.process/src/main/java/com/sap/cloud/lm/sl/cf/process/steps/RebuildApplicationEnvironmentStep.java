package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ApplicationsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("rebuildApplicationEnvironmentStep")
public class RebuildApplicationEnvironmentStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(RebuildApplicationEnvironmentStep.class);

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("rebuildApplicationEnvironmentTask").displayName("Re-Build Application Environment").description(
            "Re-Build Application Environment").build();
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws Exception {
        logActivitiTask(context, LOGGER);

        try {
            DeployedMta deployedMta = StepsUtil.getDeployedMta(context);
            List<DeployedMtaModule> deployedModules = (deployedMta != null) ? deployedMta.getModules() : Collections.emptyList();
            Set<String> mtaArchiveModules = StepsUtil.getMtaArchiveModules(context);
            debug(context, format(Messages.MTA_ARCHIVE_MODULES, mtaArchiveModules), LOGGER);
            Set<String> deployedModuleNames = CloudModelBuilderUtil.getDeployedModuleNames(deployedModules);
            debug(context, format(Messages.DEPLOYED_MODULES, deployedModuleNames), LOGGER);
            Set<String> mtaModules = StepsUtil.getMtaModules(context);
            debug(context, format(Messages.MTA_MODULES, mtaModules), LOGGER);
            // TODO: Build a cloud model only for the current application:
            List<CloudApplicationExtended> modifiedApps = getApplicationsCloudModelBuilder(context).build(mtaArchiveModules, mtaModules,
                deployedModuleNames);
            CloudApplicationExtended app = StepsUtil.getApp(context);
            CloudApplicationExtended modifiedApp = findApplication(modifiedApps, app.getName());
            app.setEnv(MapUtil.upcastUnmodifiable(modifiedApp.getEnvAsMap()));
            debug(context, format(Messages.APP_WITH_UPDATED_ENVIRONMENT, JsonUtil.toJson(app, true)), LOGGER);
            StepsUtil.setApp(context, app);
        } catch (SLException e) {
            error(context, Messages.ERROR_BUILDING_CLOUD_APP_MODEL, e, LOGGER);
            throw e;
        }
        return ExecutionStatus.SUCCESS;
    }

    private ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DelegateExecution context) {
        return StepsUtil.getApplicationsCloudModelBuilder(context);
    }

    private CloudApplicationExtended findApplication(List<CloudApplicationExtended> apps, String applicationName) {
        return apps.stream().filter(app -> app.getName().equals(applicationName)).findFirst().get();
    }

}
