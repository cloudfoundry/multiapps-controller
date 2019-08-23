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
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationState;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationGetter;
import com.sap.cloud.lm.sl.cf.process.util.ServiceProgressReporter;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Named("checkForOperationsInProgressStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CheckForOperationsInProgressStep extends AsyncFlowableStep {

    @Inject
    private ServiceOperationGetter serviceOperationGetter;
    @Inject
    private ServiceProgressReporter serviceProgressReporter;

    @Override
    protected StepPhase executeAsyncStep(ExecutionWrapper execution) throws Exception {
        List<CloudServiceExtended> servicesToProcess = getServicesToProcess(execution);

        List<CloudServiceExtended> existingServices = getExistingServices(execution.getControllerClient(), servicesToProcess);
        if (existingServices.isEmpty()) {
            return StepPhase.DONE;
        }

        Map<CloudServiceExtended, ServiceOperation> servicesInProgressState = getServicesInProgressState(execution, existingServices);
        if (servicesInProgressState.isEmpty()) {
            return StepPhase.DONE;
        }

        getStepLogger().info(Messages.WAITING_PREVIOUS_OPERATIONS_TO_FINISH);

        Map<String, ServiceOperationType> servicesOperationTypes = getServicesOperationTypes(servicesInProgressState);
        getStepLogger().debug(Messages.SERVICES_IN_PROGRESS, JsonUtil.toJson(servicesOperationTypes, true));
        StepsUtil.setTriggeredServiceOperations(execution.getContext(), servicesOperationTypes);

        List<CloudServiceExtended> servicesWithData = getListOfServicesWithData(servicesInProgressState);
        StepsUtil.setServicesData(execution.getContext(), servicesWithData);

        return StepPhase.POLL;
    }

    protected List<CloudServiceExtended> getServicesToProcess(ExecutionWrapper execution) {
        return Collections.singletonList(StepsUtil.getServiceToProcess(execution.getContext()));
    }

    private List<CloudServiceExtended> getExistingServices(CloudControllerClient cloudControllerClient,
                                                           List<CloudServiceExtended> servicesToProcess) {
        return servicesToProcess.parallelStream()
                                .map(service -> getExistingService(cloudControllerClient, service))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
    }

    private CloudServiceExtended getExistingService(CloudControllerClient cloudControllerClient, CloudServiceExtended service) {
        CloudService existingService = cloudControllerClient.getService(service.getName(), false);
        if (existingService != null) {
            return ImmutableCloudServiceExtended.builder()
                                                .from(service)
                                                .metadata(existingService.getMetadata())
                                                .build();
        }
        return null;
    }

    private Map<CloudServiceExtended, ServiceOperation> getServicesInProgressState(ExecutionWrapper execution,
                                                                                   List<CloudServiceExtended> existingServices) {
        Map<CloudServiceExtended, ServiceOperation> servicesOperation = new HashMap<>();
        for (CloudServiceExtended existingService : existingServices) {
            ServiceOperation lastServiceOperation = serviceOperationGetter.getLastServiceOperation(execution, existingService);
            if (isServiceOperationInProgress(lastServiceOperation)) {
                servicesOperation.put(existingService, lastServiceOperation);
            }
        }
        return servicesOperation;
    }

    private boolean isServiceOperationInProgress(ServiceOperation lastServiceOperation) {
        return lastServiceOperation != null && lastServiceOperation.getState() == ServiceOperationState.IN_PROGRESS;
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

    private List<CloudServiceExtended> getListOfServicesWithData(Map<CloudServiceExtended, ServiceOperation> servicesInProgressState) {
        return new ArrayList<>(servicesInProgressState.keySet());
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ExecutionWrapper execution) {
        return Arrays.asList(new PollServiceInProgressOperationsExecution(serviceOperationGetter, serviceProgressReporter));
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return Messages.ERROR_MONITORING_OPERATIONS_OVER_SERVICES;
    }

}
