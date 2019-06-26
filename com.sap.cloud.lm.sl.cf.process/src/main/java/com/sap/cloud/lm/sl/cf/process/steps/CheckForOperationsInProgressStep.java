package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.EventsGetter;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceGetter;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationState;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Component("checkForOperationsInProgressStep")
public class CheckForOperationsInProgressStep extends AsyncFlowableStep {

    @Inject
    private ServiceGetter serviceInstanceGetter;
    @Inject
    private EventsGetter eventsGetter;

    @Override
    protected StepPhase executeAsyncStep(ExecutionWrapper execution) throws Exception {
        CloudControllerClient controllerClient = execution.getControllerClient();
        String spaceId = StepsUtil.getSpaceId(execution.getContext());
        List<CloudServiceExtended> servicesToProcess = getServicesToProcess(execution);

        List<CloudServiceExtended> existingServices = getExistingServices(controllerClient, servicesToProcess);
        if (existingServices.isEmpty()) {
            return StepPhase.DONE;
        }

        Map<CloudServiceExtended, ServiceOperation> servicesInProgressState = getServicesInProgressState(existingServices, controllerClient,
                                                                                                         spaceId);
        if (servicesInProgressState.isEmpty()) {
            return StepPhase.DONE;
        }

        getStepLogger().info(Messages.POLLING_IN_PROGRESS_SERVICES);

        Map<String, ServiceOperationType> servicesOperationTypes = getServicesOperationTypes(servicesInProgressState);
        getStepLogger().debug(Messages.SERVICES_IN_PROGRESS, JsonUtil.toJson(servicesOperationTypes, true));
        StepsUtil.setTriggeredServiceOperations(execution.getContext(), servicesOperationTypes);

        List<CloudServiceExtended> servicesData = getServicesData(servicesInProgressState);
        StepsUtil.setServicesData(execution.getContext(), servicesData);

        return StepPhase.POLL;
    }

    protected List<CloudServiceExtended> getServicesToProcess(ExecutionWrapper execution) {
        return Collections.singletonList(StepsUtil.getServiceToProcess(execution.getContext()));
    }

    private List<CloudServiceExtended> getExistingServices(CloudControllerClient controllerClient,
                                                           List<CloudServiceExtended> servicesToProcess) {
        return servicesToProcess.parallelStream()
                                .map(service -> getExistingService(controllerClient, service))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
    }

    private CloudServiceExtended getExistingService(CloudControllerClient controllerClient, CloudServiceExtended service) {
        CloudService existingService = controllerClient.getService(service.getName(), false);
        if (existingService != null) {
            return ImmutableCloudServiceExtended.builder()
                                                .from(service)
                                                .metadata(existingService.getMetadata())
                                                .build();
        }
        return null;
    }

    private Map<CloudServiceExtended, ServiceOperation> getServicesInProgressState(List<CloudServiceExtended> existingServices,
                                                                                   CloudControllerClient controllerClient, String spaceId) {
        Map<CloudServiceExtended, ServiceOperation> servicesOperation = new HashMap<>();
        for (CloudServiceExtended existingService : existingServices) {
            Map<String, Object> serviceInstanceEntity = getServiceInstanceEntity(controllerClient, existingService, spaceId);
            ServiceOperation serviceOperationInProgress = getServiceOperationInProgress(serviceInstanceEntity);
            if (serviceOperationInProgress != null) {
                servicesOperation.put(existingService, serviceOperationInProgress);
            }
        }
        return servicesOperation;
    }

    private Map<String, Object> getServiceInstanceEntity(CloudControllerClient client, CloudService service, String spaceId) {
        return serviceInstanceGetter.getServiceInstanceEntity(client, service.getName(), spaceId);
    }

    private ServiceOperation getServiceOperationInProgress(Map<String, Object> serviceInstanceEntity) {
        ServiceOperation lastOpertaion = getLastOperation(serviceInstanceEntity);
        return lastOpertaion != null && lastOpertaion.getState() == ServiceOperationState.IN_PROGRESS ? lastOpertaion : null;
    }

    @SuppressWarnings("unchecked")
    private ServiceOperation getLastOperation(Map<String, Object> cloudServiceInstance) {
        Map<String, Object> lastOperationAsMap = (Map<String, Object>) MapUtils.getMap(cloudServiceInstance,
                                                                                       ServiceOperation.LAST_SERVICE_OPERATION);
        if (lastOperationAsMap == null) {
            return null;
        }
        return parseServiceOperationFromMap(lastOperationAsMap);
    }

    private ServiceOperation parseServiceOperationFromMap(Map<String, Object> serviceOperation) {
        if (serviceOperation.get(ServiceOperation.SERVICE_OPERATION_TYPE) == null
            || serviceOperation.get(ServiceOperation.SERVICE_OPERATION_STATE) == null) {
            return null;
        }
        return ServiceOperation.fromMap(serviceOperation);
    }

    private Map<String, ServiceOperationType>
            getServicesOperationTypes(Map<CloudServiceExtended, ServiceOperation> servicesInProgressState) {
        return servicesInProgressState.entrySet()
                                      .stream()
                                      .collect(Collectors.toMap(serviceName -> serviceName.getKey()
                                                                                          .getName(),
                                                                serviceOperationType -> serviceOperationType.getValue()
                                                                                                            .getType()));
    }

    private List<CloudServiceExtended> getServicesData(Map<CloudServiceExtended, ServiceOperation> servicesInProgressState) {
        return new ArrayList<>(servicesInProgressState.keySet());
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ExecutionWrapper execution) {
        return Arrays.asList(new PollServiceInProgressOperationsExecution(serviceInstanceGetter, eventsGetter));
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return Messages.ERROR_MONITORING_OPERATIONS_OVER_SERVICES;
    }

}
