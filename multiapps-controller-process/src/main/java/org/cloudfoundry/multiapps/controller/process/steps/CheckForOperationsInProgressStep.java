package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ServiceOperation;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceOperationGetter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceProgressReporter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("checkForOperationsInProgressStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CheckForOperationsInProgressStep extends AsyncFlowableStep {

    @Inject
    private ServiceOperationGetter serviceOperationGetter;
    @Inject
    private ServiceProgressReporter serviceProgressReporter;

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) {
        List<CloudServiceInstanceExtended> servicesToProcess = getServicesToProcess(context);

        List<CloudServiceInstanceExtended> existingServices = getExistingServices(context.getControllerClient(), servicesToProcess);
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

    protected List<CloudServiceInstanceExtended> getServicesToProcess(ProcessContext context) {
        return Collections.singletonList(context.getVariable(Variables.SERVICE_TO_PROCESS));
    }

    private List<CloudServiceInstanceExtended> getExistingServices(CloudControllerClient cloudControllerClient,
                                                                   List<CloudServiceInstanceExtended> servicesToProcess) {
        return servicesToProcess.parallelStream()
                                .map(service -> getExistingService(cloudControllerClient, service))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
    }

    private CloudServiceInstanceExtended getExistingService(CloudControllerClient cloudControllerClient,
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

    private Map<CloudServiceInstanceExtended, ServiceOperation>
            getServicesInProgressState(ProcessContext context, List<CloudServiceInstanceExtended> existingServices) {
        Map<CloudServiceInstanceExtended, ServiceOperation> servicesOperation = new HashMap<>();
        for (CloudServiceInstanceExtended existingService : existingServices) {
            ServiceOperation lastServiceOperation = serviceOperationGetter.getLastServiceOperation(context, existingService);
            if (isServiceOperationInProgress(lastServiceOperation)) {
                servicesOperation.put(existingService, lastServiceOperation);
            }
        }
        return servicesOperation;
    }

    private boolean isServiceOperationInProgress(ServiceOperation lastServiceOperation) {
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
