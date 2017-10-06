package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.slp.model.AsyncStepMetadata;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("startAppDeploySubProcessStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StartAppDeploySubProcessStep extends AbstractXS2SubProcessStarterStep {

    public static StepMetadata getMetadata() {
        return AsyncStepMetadata.builder().id("startSubProcessTask").displayName("Start App Deploy SubProcess").description(
            "Start App Deploy SubProcess").pollTaskId("monitorSubProcessTask").build();
    }

    protected String getIterationVariableName() {
        return Constants.VAR_APP_TO_DEPLOY;
    }

    protected String getProcessDefinitionKey() {
        return Constants.DEPLOY_APP_SUB_PROCESS_ID;
    }

    @Override
    public String getLogicalStepName() {
        return StartAppDeploySubProcessStep.class.getSimpleName();
    }

    @Override
    protected Object getIterationVariable(DelegateExecution context, int index) {
        List<CloudApplicationExtended> appsToDeploy = StepsUtil.getAppsToDeploy(context);
        return JsonUtil.toJson(appsToDeploy.get(index));
    }

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_APPS_INDEX;
    }

}
