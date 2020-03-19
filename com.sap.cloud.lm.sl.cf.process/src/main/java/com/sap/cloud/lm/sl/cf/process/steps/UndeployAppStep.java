package com.sap.cloud.lm.sl.cf.process.steps;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.process.variables.Variables;

public abstract class UndeployAppStep extends SyncFlowableStepWithHooks {

    @Override
    protected StepPhase executeStepInternal(ProcessContext context) {
        CloudApplication cloudApplicationToUndeploy = context.getVariable(Variables.APP_TO_PROCESS);
        CloudControllerClient client = context.getControllerClient();

        return undeployApplication(client, cloudApplicationToUndeploy);
    }

    protected abstract StepPhase undeployApplication(CloudControllerClient client, CloudApplication cloudApplicationToUndeploy);

}
