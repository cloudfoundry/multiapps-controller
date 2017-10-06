package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.activiti.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
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
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RebuildApplicationEnvironmentStep extends AbstractXS2ProcessStep {

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("rebuildApplicationEnvironmentTask").displayName("Re-Build Application Environment").description(
            "Re-Build Application Environment").build();
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws Exception {
        getStepLogger().logActivitiTask();

        try {
            DeployedMta deployedMta = StepsUtil.getDeployedMta(context);
            List<DeployedMtaModule> deployedModules = (deployedMta != null) ? deployedMta.getModules() : Collections.emptyList();
            Set<String> mtaArchiveModules = StepsUtil.getMtaArchiveModules(context);
            getStepLogger().debug(Messages.MTA_ARCHIVE_MODULES, mtaArchiveModules);
            Set<String> deployedModuleNames = CloudModelBuilderUtil.getDeployedModuleNames(deployedModules);
            getStepLogger().debug(Messages.DEPLOYED_MODULES, deployedModuleNames);
            Set<String> mtaModules = StepsUtil.getMtaModules(context);
            getStepLogger().debug(Messages.MTA_MODULES, mtaModules);
            // TODO: Build a cloud model only for the current application:
            List<CloudApplicationExtended> modifiedApps = getApplicationsCloudModelBuilder(context).build(mtaArchiveModules, mtaModules,
                deployedModuleNames);
            CloudApplicationExtended app = StepsUtil.getApp(context);
            CloudApplicationExtended modifiedApp = findApplication(modifiedApps, app.getName());
            app.setEnv(MapUtil.upcastUnmodifiable(modifiedApp.getEnvAsMap()));
            getStepLogger().debug(Messages.APP_WITH_UPDATED_ENVIRONMENT, JsonUtil.toJson(app, true));
            StepsUtil.setApp(context, app);
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_BUILDING_CLOUD_APP_MODEL);
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
