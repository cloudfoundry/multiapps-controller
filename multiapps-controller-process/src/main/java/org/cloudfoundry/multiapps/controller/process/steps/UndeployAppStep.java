package org.cloudfoundry.multiapps.controller.process.steps;

import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

public abstract class UndeployAppStep extends SyncFlowableStepWithHooks {

    @Override
    public StepPhase executeStepInternal(ProcessContext context) {
        CloudApplication cloudApplicationToUndeploy = context.getVariable(Variables.APP_TO_PROCESS);
        CloudControllerClient client = context.getControllerClient();

        return undeployApplication(client, cloudApplicationToUndeploy, context);
    }

    protected abstract StepPhase undeployApplication(CloudControllerClient client, CloudApplication cloudApplicationToUndeploy,
                                                     ProcessContext context);

}
