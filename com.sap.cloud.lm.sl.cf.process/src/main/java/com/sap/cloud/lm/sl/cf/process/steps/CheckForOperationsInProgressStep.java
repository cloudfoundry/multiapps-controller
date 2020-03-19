package com.sap.cloud.lm.sl.cf.process.steps;

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
import org.cloudfoundry.client.lib.domain.CloudService;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.model.ServiceOperation;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationGetter;
import com.sap.cloud.lm.sl.cf.process.util.ServiceProgressReporter;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Named("checkForOperationsInProgressStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CheckForOperationsInProgressStep extends AsyncFlowableStep {

    @Inject
    private ServiceOperationGetter serviceOperationGetter;
    @Inject
    private ServiceProgressReporter serviceProgressReporter;

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) {
        List<CloudServiceExtended> servicesToProcess = getServicesToProcess(context);

        List<CloudServiceExtended> existingServices = getExistingServices(context.getControllerClient(), servicesToProcess);
        if (existingServices.isEmpty()) {
            return StepPhase.DONE;
        }

        Map<CloudServiceExtended, ServiceOperation> servicesInProgressState = getServicesInProgressState(context, existingServices);
        if (servicesInProgressState.isEmpty()) {
            return StepPhase.DONE;
        }

        getStepLogger().info(Messages.WAITING_PREVIOUS_OPERATIONS_TO_FINISH);

        Map<String, ServiceOperation.Type> servicesOperationTypes = getServicesOperationTypes(servicesInProgressState);
        getStepLogger().debug(Messages.SERVICES_IN_PROGRESS, JsonUtil.toJson(servicesOperationTypes, true));
        context.setVariable(Variables.TRIGGERED_SERVICE_OPERATIONS, servicesOperationTypes);

        List<CloudServiceExtended> servicesWithData = getListOfServicesWithData(servicesInProgressState);
        StepsUtil.setServicesData(context.getExecution(), servicesWithData);

        return StepPhase.POLL;
    }

    protected List<CloudServiceExtended> getServicesToProcess(ProcessContext context) {
        return Collections.singletonList(context.getVariable(Variables.SERVICE_TO_PROCESS));
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

    private Map<CloudServiceExtended, ServiceOperation> getServicesInProgressState(ProcessContext context,
                                                                                   List<CloudServiceExtended> existingServices) {
        Map<CloudServiceExtended, ServiceOperation> servicesOperation = new HashMap<>();
        for (CloudServiceExtended existingService : existingServices) {
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
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return Collections.singletonList(new PollServiceInProgressOperationsExecution(serviceOperationGetter, serviceProgressReporter));
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_MONITORING_OPERATIONS_OVER_SERVICES;
    }

}
