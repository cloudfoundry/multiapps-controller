package com.sap.cloud.lm.sl.cf.process.steps;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;

public abstract class UndeployAppStep extends SyncFlowableStepWithHooks {

    @Override
    protected StepPhase executeStepInternal(ExecutionWrapper execution) {
        CloudApplication cloudApplicationToUndeploy = StepsUtil.getApp(execution.getContext());
        CloudControllerClient client = execution.getControllerClient();

        return undeployApplication(client, cloudApplicationToUndeploy);
    }

    protected abstract StepPhase undeployApplication(CloudControllerClient client, CloudApplication cloudApplicationToUndeploy);

}
