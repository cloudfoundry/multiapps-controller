package com.sap.cloud.lm.sl.cf.process.steps;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;

public abstract class UndeployAppStep extends SyncFlowableStepWithHooks {

    @Override
    protected StepPhase executeStepInternal(ExecutionWrapper execution) {
        CloudApplication cloudApplicationToUndeploy = StepsUtil.getApp(execution.getContext());
        CloudControllerClient client = execution.getControllerClient();

        return undeployApplication(client, cloudApplicationToUndeploy);
    }

    @Override
    protected void onStepError(DelegateExecution context, Exception e) throws Exception {
        throw e;
    }
    
    protected abstract StepPhase undeployApplication(CloudControllerClient client, CloudApplication cloudApplicationToUndeploy);

}
