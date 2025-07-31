package org.cloudfoundry.multiapps.controller.process.steps;

import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

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
