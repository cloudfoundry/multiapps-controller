package org.cloudfoundry.multiapps.controller.process.steps;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudEntity;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceOperationGetter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceProgressReporter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class CollectServicesInProgressStateStep extends AsyncFlowableStep {

    protected ServiceOperationGetter serviceOperationGetter;
    protected ServiceProgressReporter serviceProgressReporter;

    CollectServicesInProgressStateStep(ServiceOperationGetter serviceOperationGetter, ServiceProgressReporter serviceProgressReporter) {
        this.serviceOperationGetter = serviceOperationGetter;
        this.serviceProgressReporter = serviceProgressReporter;
    }


    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) {
        List<CloudServiceInstanceExtended> existingServicesInProgress = getExistingServicesInProgress(context);
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

    protected abstract List<CloudServiceInstanceExtended> getExistingServicesInProgress(ProcessContext context);

    protected CloudServiceInstanceExtended getExistingService(CloudControllerClient client, CloudServiceInstanceExtended service) {
        CloudServiceInstance existingService = client.getServiceInstance(service.getName(), false);
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
