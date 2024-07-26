package org.cloudfoundry.multiapps.controller.process.steps;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.model.IncrementalAppInstanceUpdateConfiguration;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

public class PollStartAppExecutionWithRollback extends PollStartAppStatusExecution {

    public PollStartAppExecutionWithRollback(CloudControllerClientFactory clientFactory, TokenService tokenService) {
        super(clientFactory, tokenService);
    }

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        AsyncExecutionState asyncExecutionState = super.execute(context);
        if (asyncExecutionState == AsyncExecutionState.ERROR) {
            rollbackOldAppInstances(context);
        }
        return asyncExecutionState;
    }

    private void rollbackOldAppInstances(ProcessContext context) {
        IncrementalAppInstanceUpdateConfiguration incrementalAppInstanceUpdateConfiguration = context.getVariable(Variables.INCREMENTAL_APP_INSTANCE_UPDATE_CONFIGURATION);
        CloudApplication oldApplication = incrementalAppInstanceUpdateConfiguration.getOldApplication();
        CloudControllerClient client = context.getControllerClient();
        context.getStepLogger()
               .warn(Messages.SCALING_DOWN_NEW_APPLICATION_0_TO_1_INSTANCES, incrementalAppInstanceUpdateConfiguration.getNewApplication()
                                                                                                                      .getName());
        client.updateApplicationInstances(incrementalAppInstanceUpdateConfiguration.getNewApplication()
                                                                                   .getName(),
                                          1);
        if (oldApplication == null) {
            return;
        }
        context.getStepLogger()
               .info(Messages.SCALING_UP_OLD_APPLICATION_0_TO_1_INSTANCES, oldApplication.getName(),
                     incrementalAppInstanceUpdateConfiguration.getOldApplicationInitialInstanceCount());
        client.updateApplicationInstances(oldApplication.getName(),
                                          incrementalAppInstanceUpdateConfiguration.getOldApplicationInitialInstanceCount());
    }

    @Override
    protected void onSuccess(ProcessContext context, String message, Object... arguments) {
        IncrementalAppInstanceUpdateConfiguration incrementalAppInstanceUpdateConfiguration = context.getVariable(Variables.INCREMENTAL_APP_INSTANCE_UPDATE_CONFIGURATION);
        CloudApplicationExtended appToProcess = context.getVariable(Variables.APP_TO_PROCESS);
        if (incrementalAppInstanceUpdateConfiguration.getNewApplicationInstanceCount() == appToProcess.getInstances()) {
            super.onSuccess(context, message, arguments);
        }
    }
}
