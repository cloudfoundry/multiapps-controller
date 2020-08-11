package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudMetadata;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public class UpdateServiceMetadataExecution implements AsyncExecution {

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        CloudServiceInstanceExtended serviceInstance = context.getVariable(Variables.SERVICE_TO_PROCESS);
        context.getStepLogger()
               .debug(Messages.UPDATING_METADATA_OF_SERVICE_INSTANCE_0, serviceInstance.getName());
        updateMetadata(context.getControllerClient(), serviceInstance);
        context.getStepLogger()
               .debug(Messages.UPDATING_METADATA_OF_SERVICE_INSTANCE_0_DONE, serviceInstance.getName());
        return AsyncExecutionState.FINISHED;
    }

    private void updateMetadata(CloudControllerClient client, CloudServiceInstanceExtended serviceInstance) {
        CloudMetadata serviceMetadata = client.getServiceInstance(serviceInstance.getName())
                                              .getMetadata();
        CloudServiceInstance serviceWithMetadata = ImmutableCloudServiceInstance.copyOf(serviceInstance)
                                                                                .withMetadata(serviceMetadata);
        client.updateServiceInstanceMetadata(serviceWithMetadata.getMetadata()
                                                                .getGuid(),
                                             serviceWithMetadata.getV3Metadata());
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        CloudServiceInstanceExtended serviceInstance = context.getVariable(Variables.SERVICE_TO_PROCESS);
        return MessageFormat.format(Messages.ERROR_UPDATING_METADATA_OF_SERVICE_INSTANCE_0, serviceInstance.getName());
    }

}
