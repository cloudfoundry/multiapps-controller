package com.sap.cloud.lm.sl.cf.process.steps;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.Constants;

@Component("prepareAppsRestartStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareAppsRestartStep extends PrepareAppsDeploymentStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        super.executeStep(execution);

        execution.getContext()
            .setVariable(Constants.REBUILD_APP_ENV, true);
        execution.getContext()
            .setVariable(Constants.SHOULD_UPLOAD_APPLICATION_CONTENT, false);
        execution.getContext()
            .setVariable(Constants.EXECUTE_ONE_OFF_TASKS, false);
        StepsUtil.setUseIdleUris(execution.getContext(), false);

        return StepPhase.DONE;
    }

}
