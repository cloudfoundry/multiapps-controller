package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.model.ZdmActionEnum;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.LoopStepMetadata;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("hdiDeployerAppsFinalizeStep")
public class HdiDeployerAppsFinalizeStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(HdiDeployerAppsFinalizeStep.class);

    private static final StepMetadata STEP_POLL_RESTART_ZDM_DEPLOYER = StepMetadata.builder().id(
        "pollRestartHdiDeployerStatusTask").displayName("Poll Restart ZDM HDI Deployer").description(
            "Poll Restart ZDM HDI Deployer").build();

    private static final StepMetadata STEP_POLL_RE_EXECUTE_ZDM_DEPLOYER = StepMetadata.builder().id(
        "pollReExecuteHdiDeployerStatusTask").displayName("Poll Re-Execute ZDM HDI Deployer Status").description(
            "Poll Re-Execute ZDM HDI Deployer Status").build();

    private static final StepMetadata STEP_RESTART_ZDM_DEPLOYER = StepMetadata.builder().id("restartZdmHdiDeployerAppTask").displayName(
        "Restart ZDM HDI Deployer").description("Restart ZDM HDI Deployer").children(STEP_POLL_RESTART_ZDM_DEPLOYER,
            STEP_POLL_RE_EXECUTE_ZDM_DEPLOYER).build();

    public static StepMetadata getMetadata() {
        return LoopStepMetadata.builder().id("hdiDeployerFinalizeTask").displayName("Hdi Deployer Finalize").description(
            "Hdi Deployer Finalize").children(STEP_RESTART_ZDM_DEPLOYER).countVariable(Constants.VAR_APPS_COUNT).build();
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws Exception {
        logActivitiTask(context, LOGGER);

        try {
            List<CloudApplicationExtended> hdiDeployerAppsInZdmMode = StepsUtil.getHdiDeployerAppsInZdmMode(context);
            List<String> hdiDeployerAppNames = new ArrayList<String>();
            CloudFoundryOperations client = getCloudFoundryClient(context, LOGGER);

            for (CloudApplicationExtended hdiDeployerApp : hdiDeployerAppsInZdmMode) {
                Map<String, String> hdiDeployerEnvVars = hdiDeployerApp.getEnvAsMap();
                hdiDeployerEnvVars.put(ZdmActionEnum.ZDM_ACTION.toString(), ZdmActionEnum.FINALIZE.toString());
                client.updateApplicationEnv(hdiDeployerApp.getName(), hdiDeployerEnvVars);
                hdiDeployerAppNames.add(hdiDeployerApp.getName());
            }

            context.setVariable(Constants.VAR_APPS_INDEX, 0);
            context.setVariable(Constants.VAR_INDEX_VARIABLE_NAME, Constants.VAR_APPS_INDEX);
            StepsUtil.setAppsToRestart(context, hdiDeployerAppNames);
            context.setVariable(Constants.VAR_APPS_COUNT, hdiDeployerAppNames.size());
        } catch (CloudFoundryException exception) {
            SLException e = StepsUtil.createException(exception);
            error(context, Messages.ERROR_RECONFIGURING_APPS_ENVIRONMENTS, LOGGER);
            throw e;
        }

        return ExecutionStatus.SUCCESS;
    }
}
