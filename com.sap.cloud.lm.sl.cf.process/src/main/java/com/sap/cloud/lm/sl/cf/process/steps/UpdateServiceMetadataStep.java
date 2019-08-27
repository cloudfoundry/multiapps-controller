package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudServiceBrokerException;
import org.cloudfoundry.client.lib.domain.CloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudService;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceWithAlternativesCreator;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution;
import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution.ExecutionState;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Component("updateServiceMetadataStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateServiceMetadataStep extends ServiceStep {

    @Override
    protected MethodExecution<String> executeOperation(DelegateExecution execution, CloudControllerClient controllerClient,
                                                       CloudServiceExtended service) {
        return updateServiceMetadata(execution, controllerClient, service);
    }

    private MethodExecution<String> updateServiceMetadata(DelegateExecution context, CloudControllerClient client, CloudServiceExtended service) {
        getStepLogger().info(Messages.UPDATING_SERVICE_METADATA, service.getName(), service.getResourceName());
        updateServiceMetadata(service, client);
        getStepLogger().debug(Messages.SERVICE_METADATA_UPDATED, service.getName());
        return new MethodExecution<>(null, ExecutionState.FINISHED);
    }

    private void updateServiceMetadata(CloudServiceExtended serviceToProcess, CloudControllerClient client) {
        ImmutableCloudService serviceWithMetadata = ImmutableCloudService.copyOf(serviceToProcess);
        if(serviceToProcess.getMetadata() == null || serviceToProcess.getMetadata().getGuid() == null) {
            CloudMetadata serviceMeta = client.getService(serviceWithMetadata.getName()).getMetadata();
            serviceWithMetadata = serviceWithMetadata.withMetadata(serviceMeta);
        }
        client.updateServiceMetadata(serviceWithMetadata.getMetadata().getGuid(), serviceWithMetadata.getV3Metadata());
        getStepLogger().info("updated service metadata name: " + serviceWithMetadata + " metadata: " + JsonUtil.toJson(serviceWithMetadata.getV3Metadata(), true));
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ExecutionWrapper execution) {
        return Arrays.asList(new PollServiceCreateOrUpdateOperationsExecution(getServiceOperationGetter(), getServiceProgressReporter()));
    }

    @Override
    protected ServiceOperationType getOperationType() {
        return ServiceOperationType.UPDATE;
    }

}
