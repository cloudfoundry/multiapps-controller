package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sap.cloudfoundry.client.facade.domain.CloudEntity;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceOperationGetter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceProgressReporter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;

@Named("checkForOperationsInProgressStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CheckForOperationsInProgressStep extends AsyncFlowableStep {

    @Inject
    protected ServiceOperationGetter serviceOperationGetter;
    @Inject
    private ServiceProgressReporter serviceProgressReporter;

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) {
        List<CloudServiceInstanceExtended> existingServicesInProgress = getExistingServiceInProgress(context);
        if (existingServicesInProgress.isEmpty()) {
            return StepPhase.DONE;
        }

        getStepLogger().info(Messages.WAITING_PREVIOUS_OPERATIONS_TO_FINISH);

        Map<String, ServiceOperation.Type> servicesOperationTypes = getServicesOperationTypes(existingServicesInProgress);
        getStepLogger().debug(Messages.SERVICES_IN_PROGRESS, JsonUtil.toJson(servicesOperationTypes, true));
        context.setVariable(Variables.TRIGGERED_SERVICE_OPERATIONS, servicesOperationTypes);

        context.setVariable(Variables.SERVICES_DATA, existingServicesInProgress);

        return StepPhase.POLL;
    }

    protected List<CloudServiceInstanceExtended> getExistingServiceInProgress(ProcessContext context) {
        CloudControllerClient client = context.getControllerClient();
        CloudServiceInstanceExtended serviceToProcess = context.getVariable(Variables.SERVICE_TO_PROCESS);
        CloudServiceInstanceExtended existingServiceInstance = getExistingService(client, serviceToProcess);
        if (existingServiceInstance == null || !isServiceOperationInProgress(existingServiceInstance)) {
            return Collections.emptyList();
        }
        return List.of(existingServiceInstance);
    }

    protected CloudServiceInstanceExtended getExistingService(CloudControllerClient cloudControllerClient,
                                                              CloudServiceInstanceExtended service) {
        CloudServiceInstance existingService = cloudControllerClient.getServiceInstance(service.getName(), false);
        if (existingService != null) {
            return ImmutableCloudServiceInstanceExtended.builder()
                                                        .from(service)
                                                        .from(existingService)
                                                        .metadata(existingService.getMetadata())
                                                        .build();
        }
        return null;
    }

    protected boolean isServiceOperationInProgress(CloudServiceInstanceExtended service) {
        ServiceOperation lastServiceOperation = service.getLastOperation();
        return lastServiceOperation != null && lastServiceOperation.getState() == ServiceOperation.State.IN_PROGRESS;
    }

    private Map<String, ServiceOperation.Type> getServicesOperationTypes(List<CloudServiceInstanceExtended> servicesInProgressState) {
        return servicesInProgressState.stream()
                                      .collect(Collectors.toMap(CloudEntity::getName, service -> service.getLastOperation()
                                                                                                        .getType()));
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return Collections.singletonList(new PollServiceInProgressOperationsExecution(serviceOperationGetter, serviceProgressReporter));
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_MONITORING_OPERATIONS_OVER_SERVICES;
    }

}
