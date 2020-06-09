package com.sap.cloud.lm.sl.cf.process.util;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.variable.api.history.HistoricVariableInstance;

import com.sap.cloud.lm.sl.cf.core.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.model.Phase;
import com.sap.cloud.lm.sl.cf.core.persistence.OrderDirection;
import com.sap.cloud.lm.sl.cf.core.persistence.service.HistoricOperationEventService;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableOperation;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.common.ConflictException;

@Named("applicationColorDetector")
public class ApplicationColorDetector {

    @Inject
    private OperationService operationService;

    @Inject
    private HistoricOperationEventService historicOperationEventService;

    @Inject
    private FlowableFacade flowableFacade;

    public ApplicationColor detectLiveApplicationColor(DeployedMta deployedMta, String correlationId) {
        if (deployedMta == null) {
            return null;
        }
        ApplicationColor olderApplicationColor = getOlderApplicationColor(deployedMta);

        Optional<Operation> operationWithFinalState = getLastOperationsWithFinalState(correlationId);

        if (!operationWithFinalState.isPresent()) {
            return olderApplicationColor;
        }

        if (operationWithFinalState.get()
                                   .getState() != Operation.State.ABORTED) {
            return olderApplicationColor;
        }
        String historicProcessInstanceId = operationWithFinalState.get()
                                                                  .getProcessId();

        ApplicationColor latestDeployedColor = getColorFromHistoricProcess(historicProcessInstanceId);
        Phase phase = getPhaseFromHistoricProcess(historicProcessInstanceId);

        if (latestDeployedColor == null) {
            return olderApplicationColor;
        }
        return phase == Phase.UNDEPLOY ? latestDeployedColor : latestDeployedColor.getAlternativeColor();
    }

    public ApplicationColor detectSingularDeployedApplicationColor(DeployedMta deployedMta) {
        if (deployedMta == null) {
            return null;
        }
        ApplicationColor deployedApplicationColor = null;
        for (DeployedMtaApplication deployedMtaApplication : deployedMta.getApplications()) {
            ApplicationColor applicationColor = CloudModelBuilderUtil.getApplicationColor(deployedMtaApplication);
            if (deployedApplicationColor == null) {
                deployedApplicationColor = (applicationColor);
            }
            if (deployedApplicationColor != applicationColor) {
                throw new ConflictException(Messages.CONFLICTING_APP_COLORS,
                                            deployedMta.getMetadata()
                                                       .getId());
            }
        }
        return deployedApplicationColor;
    }

    private ApplicationColor getOlderApplicationColor(DeployedMta deployedMta) {
        return deployedMta.getApplications()
                          .stream()
                          .filter(application -> application.getMetadata() != null)
                          .min(Comparator.comparing(application -> application.getMetadata()
                                                                              .getCreatedAt()))
                          .map(CloudModelBuilderUtil::getApplicationColor)
                          .orElse(null);
    }

    private Optional<Operation> getLastOperationsWithFinalState(String correlationId) {
        List<Operation> operations = getOperations(correlationId);

        Map<String, List<HistoricOperationEvent>> operationsWithHistoricEvents = operations.stream()
                                                                                           .filter(operation -> operation.getState() == null)
                                                                                           .collect(Collectors.toMap(Operation::getProcessId,
                                                                                                                     this::getOperationHistoricEvents));

        return operations.stream()
                         .map(operation -> getFinalState(operation, operationsWithHistoricEvents))
                         .filter(operation -> operation.getState() != null)
                         .sorted(Comparator.comparing(Operation::getEndedAt)
                                           .reversed())
                         .findFirst();
    }

    private List<Operation> getOperations(String correlationId) {
        Operation currentOperation = operationService.createQuery()
                                                     .processId(correlationId)
                                                     .singleResult();

        return operationService.createQuery()
                               .mtaId(currentOperation.getMtaId())
                               .processType(ProcessType.BLUE_GREEN_DEPLOY)
                               .spaceId(currentOperation.getSpaceId())
                               .orderByEndTime(OrderDirection.DESCENDING)
                               .list();
    }

    private List<HistoricOperationEvent> getOperationHistoricEvents(Operation operation) {
        return historicOperationEventService.createQuery()
                                            .processId(operation.getProcessId())
                                            .list();
    }

    private Operation getFinalState(Operation operation, Map<String, List<HistoricOperationEvent>> operationsWithHistoricEvents) {
        List<HistoricOperationEvent> historicOperationEvents = operationsWithHistoricEvents.getOrDefault(operation.getProcessId(),
                                                                                                         Collections.emptyList());
        Optional<HistoricOperationEvent> finalOperationEvent = historicOperationEvents.stream()
                                                                                      .filter(event -> event.getType() == HistoricOperationEvent.EventType.ABORTED
                                                                                          || event.getType() == HistoricOperationEvent.EventType.FINISHED)
                                                                                      .findFirst();
        if (finalOperationEvent.isPresent()) {
            return ImmutableOperation.copyOf(operation)
                                     .withState(Operation.State.fromValue(finalOperationEvent.get()
                                                                                             .getType()
                                                                                             .toString()))
                                     .withEndedAt(ZonedDateTime.ofInstant(finalOperationEvent.get()
                                                                                             .getTimestamp()
                                                                                             .toInstant(),
                                                                          ZoneOffset.UTC));
        }
        return operation;
    }

    private ApplicationColor getColorFromHistoricProcess(String processInstanceId) {
        HistoricVariableInstance colorVariableInstance = flowableFacade.getHistoricVariableInstance(processInstanceId,
                                                                                                    Variables.IDLE_MTA_COLOR.getName());

        if (colorVariableInstance == null) {
            return null;
        }

        return ApplicationColor.valueOf((String) colorVariableInstance.getValue());
    }

    private Phase getPhaseFromHistoricProcess(String processInstanceId) {
        HistoricVariableInstance phaseVariableInstance = flowableFacade.getHistoricVariableInstance(processInstanceId,
                                                                                                    Variables.PHASE.getName());
        if (phaseVariableInstance == null) {
            return null;
        }

        return Phase.valueOf((String) phaseVariableInstance.getValue());
    }

}
