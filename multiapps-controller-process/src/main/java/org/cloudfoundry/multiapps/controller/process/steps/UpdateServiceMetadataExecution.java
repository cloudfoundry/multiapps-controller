package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.UUID;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;

public class UpdateServiceMetadataExecution implements AsyncExecution {

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        CloudServiceInstanceExtended serviceInstance = context.getVariable(Variables.SERVICE_TO_PROCESS);
        context.getStepLogger()
               .debug(Messages.UPDATING_METADATA_OF_SERVICE_INSTANCE_0, serviceInstance.getName());
        updateMetadata(context.getControllerClient(), serviceInstance, context.getStepLogger());
        context.getStepLogger()
               .debug(Messages.UPDATING_METADATA_OF_SERVICE_INSTANCE_0_DONE, serviceInstance.getName());
        return AsyncExecutionState.FINISHED;
    }

    private void updateMetadata(CloudControllerClient client, CloudServiceInstanceExtended serviceInstance, StepLogger stepLogger) {
        try {
            UUID serviceInstanceGuid = client.getRequiredServiceInstanceGuid(serviceInstance.getName());
            client.updateServiceInstanceMetadata(serviceInstanceGuid, serviceInstance.getV3Metadata());
        } catch (CloudOperationException e) {
            if (!serviceInstance.isOptional()) {
                throw new SLException(e, e.getMessage());
            }
            stepLogger.warnWithoutProgressMessage(e, Messages.METADATA_UPDATE_OF_OPTIONAL_SERVICE_INSTANCE_0_FAILED,
                                                  serviceInstance.getName());
        }
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        CloudServiceInstanceExtended serviceInstance = context.getVariable(Variables.SERVICE_TO_PROCESS);
        return MessageFormat.format(Messages.ERROR_UPDATING_METADATA_OF_SERVICE_INSTANCE_0, serviceInstance.getName());
    }

}
