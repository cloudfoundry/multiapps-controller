package org.cloudfoundry.multiapps.controller.process.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.ErrorType;
import org.cloudfoundry.multiapps.controller.api.model.FailureReason;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableDryRunResult;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableFailureReason;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.process.client.DryRunChangeSet;
import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.metadata.ProcessTypeToOperationMetadataMapper;
import org.cloudfoundry.multiapps.controller.process.variables.Serializer;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.variable.api.history.HistoricVariableInstance;

@Named
public class OperationsHelper {

    private final OperationService operationService;
    private final ProcessTypeToOperationMetadataMapper metadataMapper;
    private final ProcessHelper processHelper;
    private final FlowableFacade flowableFacade;

    @Inject
    public OperationsHelper(OperationService operationService, ProcessTypeToOperationMetadataMapper metadataMapper,
                            ProcessHelper processHelper, FlowableFacade flowableFacade) {
        this.operationService = operationService;
        this.metadataMapper = metadataMapper;
        this.processHelper = processHelper;
        this.flowableFacade = flowableFacade;
    }

    public String getProcessDefinitionKey(Operation operation) {
        return metadataMapper.getDiagramId(operation.getProcessType());
    }

    public Operation addErrorType(Operation operation) {
        if (operation.getState() == Operation.State.ERROR) {
            return ImmutableOperation.copyOf(operation)
                                     .withErrorType(getErrorType(operation))
                                     .withFailureReason(getFailureReason(operation));
        }
        return operation;
    }

    public Operation addDryRunResult(Operation operation) {
        List<DryRunChangeSet> changeSets = findAllHistoricVariableValues(operation.getProcessId(),
                                                                         Variables.DRY_RUN_CHANGE_SET.getName(),
                                                                         Variables.DRY_RUN_CHANGE_SET.getSerializer());
        if (changeSets.isEmpty()) {
            return operation;
        }
        DryRunChangeSet merged = new DryRunChangeSet();
        for (DryRunChangeSet cs : changeSets) {
            addDistinct(merged.appsCreate, cs.appsCreate);
            addDistinct(merged.appsUpdate, cs.appsUpdate);
            addDistinct(merged.appsDelete, cs.appsDelete);
            addDistinct(merged.servicesCreate, cs.servicesCreate);
            addDistinct(merged.servicesUpdate, cs.servicesUpdate);
            addDistinct(merged.servicesDelete, cs.servicesDelete);
            addDistinct(merged.routesCreate, cs.routesCreate);
            addDistinct(merged.routesDelete, cs.routesDelete);
        }
        return ImmutableOperation.copyOf(operation)
                                 .withDryRunResult(ImmutableDryRunResult.builder()
                                                                        .appsToCreate(merged.appsCreate)
                                                                        .appsToUpdate(merged.appsUpdate)
                                                                        .appsToDelete(merged.appsDelete)
                                                                        .servicesToCreate(merged.servicesCreate)
                                                                        .servicesToUpdate(merged.servicesUpdate)
                                                                        .servicesToDelete(merged.servicesDelete)
                                                                        .routesToCreate(merged.routesCreate)
                                                                        .routesToDelete(merged.routesDelete)
                                                                        .build());
    }

    private static <T> void addDistinct(List<T> target, List<T> source) {
        for (T item : source) {
            if (!target.contains(item)) {
                target.add(item);
            }
        }
    }

    public Operation addOutputs(Operation operation) {
        HistoricVariableInstance variable = findHistoricVariable(operation.getProcessId(), Variables.DEPLOYMENT_OUTPUTS.getName());
        if (variable == null || variable.getValue() == null) {
            return operation;
        }
        Map<String, String> outputs = Variables.DEPLOYMENT_OUTPUTS.getSerializer()
                                                                  .deserialize(variable.getValue());
        if (outputs == null || outputs.isEmpty()) {
            return operation;
        }
        return ImmutableOperation.copyOf(operation)
                                 .withOutputs(outputs);
    }

    public Operation releaseLockIfNeeded(Operation operation) {
        // Fixes bug XSBUG-2035: Inconsistency in 'operation', 'act_hi_procinst' and 'act_ru_execution' tables
        if (operation.hasAcquiredLock() && operation.getState()
                                                    .isFinal()) {
            operation = ImmutableOperation.builder()
                                          .from(operation)
                                          .hasAcquiredLock(false)
                                          .build();
            operationService.update(operation, operation);
        }
        return operation;
    }

    public List<Operation> releaseLocksIfNeeded(List<Operation> operations) {
        return operations.stream()
                         .map(this::releaseLockIfNeeded)
                         .collect(Collectors.toList());
    }

    private HistoricVariableInstance findHistoricVariable(String processId, String variableName) {
        HistoricVariableInstance variable = flowableFacade.getHistoricVariableInstance(processId, variableName);
        if (variable != null) {
            return variable;
        }
        for (String subProcessId : flowableFacade.getHistoricSubProcessIds(processId)) {
            variable = flowableFacade.getHistoricVariableInstance(subProcessId, variableName);
            if (variable != null) {
                return variable;
            }
        }
        return null;
    }

    private <T> List<T> findAllHistoricVariableValues(String processId, String variableName,
                                                      Serializer<T> serializer) {
        List<T> results = new ArrayList<>();
        collectHistoricVariableValues(processId, variableName, serializer, results);
        return results;
    }

    private <T> void collectHistoricVariableValues(String processId, String variableName,
                                                   Serializer<T> serializer,
                                                   List<T> results) {
        HistoricVariableInstance variable = flowableFacade.getHistoricVariableInstance(processId, variableName);
        if (variable != null && variable.getValue() != null) {
            results.add(serializer.deserialize(variable.getValue()));
        }
        for (String subProcessId : flowableFacade.getHistoricSubProcessIds(processId)) {
            collectHistoricVariableValues(subProcessId, variableName, serializer, results);
        }
    }

    private ErrorType getErrorType(Operation operation) {
        List<HistoricOperationEvent> historicEvents = processHelper.getHistoricOperationEventByProcessId(operation.getProcessId());
        if (historicEvents.isEmpty()) {
            return null;
        }
        HistoricOperationEvent.EventType lastEventType = historicEvents.get(historicEvents.size() - 1)
                                                                       .getType();
        return toErrorType(lastEventType);
    }

    private ErrorType toErrorType(HistoricOperationEvent.EventType historicType) {
        if (historicType == HistoricOperationEvent.EventType.FAILED_BY_CONTENT_ERROR) {
            return ErrorType.CONTENT;
        }
        if (historicType == HistoricOperationEvent.EventType.FAILED_BY_INFRASTRUCTURE_ERROR) {
            return ErrorType.INFRASTRUCTURE;
        }
        return null;
    }

    private FailureReason getFailureReason(Operation operation) {
        HistoricVariableInstance variable = flowableFacade.getHistoricVariableInstance(operation.getProcessId(),
                                                                                       Variables.FAILURE_REASON_CODE.getName());
        if (variable == null || variable.getValue() == null) {
            return null;
        }
        FailureReason.Code code = FailureReason.Code.valueOf(variable.getValue().toString());
        return ImmutableFailureReason.builder()
                                     .code(code)
                                     .retryable(isRetryable(code))
                                     .build();
    }

    private boolean isRetryable(FailureReason.Code code) {
        return code == FailureReason.Code.SERVICE_OPERATION_FAILURE
            || code == FailureReason.Code.CF_API_UNAVAILABLE
            || code == FailureReason.Code.INTERNAL_ERROR;
    }
}
