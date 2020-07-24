package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceInstanceExtended;
import com.sap.cloud.lm.sl.cf.core.model.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.util.MethodExecution;
import com.sap.cloud.lm.sl.cf.process.Messages;

@Named("updateServiceMetadataStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateServiceMetadataStep extends ServiceStep {

    @Override
    protected MethodExecution<String> executeOperation(ProcessContext context, CloudControllerClient controllerClient,
                                                       CloudServiceInstanceExtended service) {
        return updateServiceMetadata(controllerClient, service);
    }

    private MethodExecution<String> updateServiceMetadata(CloudControllerClient controllerClient, CloudServiceInstanceExtended service) {
        getStepLogger().debug(Messages.UPDATING_SERVICE_METADATA, service.getName(), service.getResourceName());
        updateServiceMetadata(service, controllerClient);
        getStepLogger().debug(Messages.SERVICE_METADATA_UPDATED, service.getName());
        return new MethodExecution<>(null, MethodExecution.ExecutionState.FINISHED);
    }

    private void updateServiceMetadata(CloudServiceInstanceExtended serviceToProcess, CloudControllerClient client) {
        UUID serviceGuid = client.getServiceInstance(serviceToProcess.getName())
                                 .getMetadata()
                                 .getGuid();
        client.updateServiceInstanceMetadata(serviceGuid, serviceToProcess.getV3Metadata());
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return Collections.singletonList(new PollServiceCreateOrUpdateOperationsExecution(getServiceOperationGetter(),
                                                                                          getServiceProgressReporter()));
    }

    @Override
    protected ServiceOperation.Type getOperationType() {
        return ServiceOperation.Type.UPDATE;
    }

}
