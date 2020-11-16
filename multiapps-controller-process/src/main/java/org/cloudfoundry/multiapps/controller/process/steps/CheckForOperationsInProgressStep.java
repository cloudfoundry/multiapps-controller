package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

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
        Set<CloudServiceInstanceExtended> existingServices = getExistingServicesToProcess(context);
        if (existingServices.isEmpty()) {
            return StepPhase.DONE;
        }

        Map<CloudServiceInstanceExtended, ServiceOperation> servicesInProgressState = getServicesInProgressState(context, existingServices);
        if (servicesInProgressState.isEmpty()) {
            return StepPhase.DONE;
        }

        getStepLogger().info(Messages.WAITING_PREVIOUS_OPERATIONS_TO_FINISH);

        Map<String, ServiceOperation.Type> servicesOperationTypes = getServicesOperationTypes(servicesInProgressState);
        getStepLogger().debug(Messages.SERVICES_IN_PROGRESS, JsonUtil.toJson(servicesOperationTypes, true));
        context.setVariable(Variables.TRIGGERED_SERVICE_OPERATIONS, servicesOperationTypes);

        List<CloudServiceInstanceExtended> servicesWithData = getListOfServicesWithData(servicesInProgressState);
        context.setVariable(Variables.SERVICES_DATA, servicesWithData);

        return StepPhase.POLL;
    }

    protected Set<CloudServiceInstanceExtended> getExistingServicesToProcess(ProcessContext context) {
        CloudControllerClient client = context.getControllerClient();
        CloudServiceInstanceExtended serviceToProcess = context.getVariable(Variables.SERVICE_TO_PROCESS);
        CloudServiceInstanceExtended existingServiceInstance = getExistingService(client, serviceToProcess);
        if (existingServiceInstance == null) {
            return Collections.emptySet();
        }
        return Set.of(existingServiceInstance);
    }

    protected CloudServiceInstanceExtended getExistingService(CloudControllerClient cloudControllerClient,
                                                              CloudServiceInstanceExtended service) {
        CloudServiceInstance existingService = cloudControllerClient.getServiceInstance(service.getName(), false);
        if (existingService != null) {
            return ImmutableCloudServiceInstanceExtended.builder()
                                                        .from(service)
                                                        .metadata(existingService.getMetadata())
                                                        .build();
        }
        return null;
    }

    protected Map<CloudServiceInstanceExtended, ServiceOperation>
            getServicesInProgressState(ProcessContext context, Set<CloudServiceInstanceExtended> existingServices) {
        Map<CloudServiceInstanceExtended, ServiceOperation> servicesOperation = new HashMap<>();
        CloudControllerClient client = context.getControllerClient();
        for (CloudServiceInstanceExtended existingService : existingServices) {
            ServiceOperation lastServiceOperation = serviceOperationGetter.getLastServiceOperation(client, existingService);
            if (isServiceOperationInProgress(lastServiceOperation)) {
                servicesOperation.put(existingService, lastServiceOperation);
            }
        }
        return servicesOperation;
    }

    protected boolean isServiceOperationInProgress(ServiceOperation lastServiceOperation) {
        return lastServiceOperation != null && lastServiceOperation.getState() == ServiceOperation.State.IN_PROGRESS;
    }

    private Map<String, ServiceOperation.Type>
            getServicesOperationTypes(Map<CloudServiceInstanceExtended, ServiceOperation> servicesInProgressState) {
        return servicesInProgressState.entrySet()
                                      .stream()
                                      .collect(Collectors.toMap(serviceName -> serviceName.getKey()
                                                                                          .getName(),
                                                                serviceOperationType -> serviceOperationType.getValue()
                                                                                                            .getType()));
    }

    private List<CloudServiceInstanceExtended>
            getListOfServicesWithData(Map<CloudServiceInstanceExtended, ServiceOperation> servicesInProgressState) {
        return new ArrayList<>(servicesInProgressState.keySet());
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
