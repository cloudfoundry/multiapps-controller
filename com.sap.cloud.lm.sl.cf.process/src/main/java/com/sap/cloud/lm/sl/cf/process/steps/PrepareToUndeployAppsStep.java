package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.slp.model.LoopStepMetadata;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("prepareToUndeployAppsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareToUndeployAppsStep extends AbstractXS2ProcessStep {

    public static StepMetadata getMetadata() {
        return LoopStepMetadata.builder().id("prepareToUndeployAppsTask").displayName("Prepare To Undeploy Apps").description(
            "Prepare To Undeploy Apps").children(UndeployAppStep.getMetadata()).countVariable(Constants.VAR_APPS_TO_UNDEPLOY_COUNT).build();
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) {
        getStepLogger().logActivitiTask();

        List<CloudApplication> appsToUndeploy = StepsUtil.getAppsToUndeploy(context);
        getStepLogger().debug(Messages.APPS_TO_UNDEPLOY, JsonUtil.toJson(appsToUndeploy, true));
        prepareAppsToUndeploy(context, appsToUndeploy);
        return ExecutionStatus.SUCCESS;

    }

    private void prepareAppsToUndeploy(DelegateExecution context, List<CloudApplication> appsToUndeploy) {
        context.setVariable(Constants.VAR_APPS_TO_UNDEPLOY_COUNT, appsToUndeploy.size());
        context.setVariable(Constants.VAR_APPS_TO_UNDEPLOY_INDEX, 0);
        context.setVariable(Constants.VAR_INDEX_VARIABLE_NAME, Constants.VAR_APPS_TO_UNDEPLOY_INDEX);
    }

}
