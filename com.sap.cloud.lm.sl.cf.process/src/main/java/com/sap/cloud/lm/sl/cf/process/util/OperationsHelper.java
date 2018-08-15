package com.sap.cloud.lm.sl.cf.process.util;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.activiti.ActivitiFacade;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OperationFilter;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.metadata.ProcessTypeToOperationMetadataMapper;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.common.SLException;

@Component
public class OperationsHelper {

    @Inject
    private OperationDao dao;

    @Inject
    private ProcessTypeToOperationMetadataMapper metadataMapper;

    @Inject
    private ActivitiFacade activitiFacade;

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationsHelper.class);

    public List<Operation> findOperations(OperationFilter operationFilter, List<State> statusList) {
        List<Operation> operations = dao.find(operationFilter);
        addOngoingOperationsState(operations);
        List<Operation> result = filterBasedOnStates(operations, statusList);
        return result;
    }

    public String getProcessDefinitionKey(Operation operation) {
        return metadataMapper.getActivitiDiagramId(operation.getProcessType());
    }

    public List<String> getAllProcessDefinitionKeys(Operation operation) {
        List<String> processDefinitionKeys = new ArrayList<>();
        ProcessType processType = operation.getProcessType();
        processDefinitionKeys.add(metadataMapper.getActivitiDiagramId(processType));
        processDefinitionKeys.addAll(metadataMapper.getPreviousActivitiDiagramIds(processType));
        return processDefinitionKeys;
    }

    private void addOngoingOperationsState(List<Operation> existingOngoingOperations) {
        for (Operation ongoingOperation : existingOngoingOperations) {
            addState(ongoingOperation);
        }
    }

    public void addState(Operation ongoingOperation) throws SLException {
        ongoingOperation.setState(getOngoingOperationState(ongoingOperation));
    }

    protected State getOngoingOperationState(Operation ongoingOperation) throws SLException {
        if (ongoingOperation.getState() != null) {
            return ongoingOperation.getState();
        }
        State state = computeState(ongoingOperation);
        // Fixes bug XSBUG-2035: Inconsistency in 'operation', 'act_hi_procinst' and 'act_ru_execution' tables
        if (ongoingOperation.hasAcquiredLock() && (state.equals(State.ABORTED) || state.equals(State.FINISHED))) {
            ongoingOperation.acquiredLock(false);
            ongoingOperation.setState(state);
            this.dao.merge(ongoingOperation);
        }
        return state;
    }

    public State computeState(Operation ongoingOperation) throws SLException {
        LOGGER.debug(MessageFormat.format(Messages.COMPUTING_STATE_OF_OPERATION, ongoingOperation.getProcessType(),
            ongoingOperation.getProcessId()));
        return activitiFacade.getOngoingOperationState(ongoingOperation);
    }

    private List<Operation> filterBasedOnStates(List<Operation> operations, List<State> statusList) {
        if (CollectionUtils.isEmpty(statusList)) {
            return operations;
        }
        return operations.stream()
            .filter(operation -> statusList.contains(operation.getState()))
            .collect(Collectors.toList());
    }

}
