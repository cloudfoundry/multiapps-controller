package com.sap.cloud.lm.sl.cf.process.steps;

import com.sap.cloud.lm.sl.cf.process.Constants;

public class PrepareAppsRestartStep extends PrepareAppsDeploymentStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        super.executeStep(execution);

        execution.getContext().setVariable(Constants.REBUILD_APP_ENV, true);
        execution.getContext().setVariable(Constants.SHOULD_UPLOAD_APPLICATION_CONTENT, false);
        execution.getContext().setVariable(Constants.EXECUTE_ONE_OFF_TASKS, false);
        StepsUtil.setUseIdleUris(execution.getContext(), false);

        return StepPhase.DONE;
    }

}
