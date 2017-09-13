package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.process.Constants;

@Component("prepareAppsRestartStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareAppsRestartStep extends PrepareAppsDeploymentStep {

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) {
        super.executeStepInternal(context);

        context.setVariable(Constants.REBUILD_APP_ENV, true);
        context.setVariable(Constants.SHOULD_UPLOAD_APPLICATION_CONTENT, false);
        context.setVariable(Constants.EXECUTE_ONE_OFF_TASKS, false);

        return ExecutionStatus.SUCCESS;
    }

}
