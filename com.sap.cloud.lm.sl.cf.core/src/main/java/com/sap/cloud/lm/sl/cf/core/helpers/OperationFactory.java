package com.sap.cloud.lm.sl.cf.core.helpers;

import java.text.MessageFormat;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.cf.web.api.model.State;

@Component
public class OperationFactory {

    private static final String DEPLOY_SERIALIZED_NAME = "deploy";
    private static final String BLUE_GREEN_DEPLOY_SERIALIZED_NAME = "blue-green-deploy";
    private static final String UNDEPLOY_SERIALIZED_NAME = "undeploy";

    public Operation fromSerializationDto(com.sap.cloud.lm.sl.cf.core.dto.serialization.OperationDto dto) {
        String processId = dto.getProcessId();
        ProcessType processType = deserializeProcessType(dto.getProcessType());
        String startedAt = dto.getStartedAt();
        String spaceId = dto.getSpaceId();
        String mtaId = dto.getMtaId();
        String user = dto.getUser();
        boolean acquiredLock = dto.hasAcquiredLock();
        State finalState = toState(dto.getState());
        return new Operation(processId, processType, startedAt, spaceId, mtaId, user, acquiredLock, finalState);
    }

    public Operation fromPersistenceDto(com.sap.cloud.lm.sl.cf.core.dto.persistence.OperationDto dto) {
        String processId = dto.getProcessId();
        ProcessType processType = toProcessType(dto.getProcessType());
        String startedAt = dto.getStartedAt();
        String spaceId = dto.getSpaceId();
        String mtaId = dto.getMtaId();
        String user = dto.getUser();
        boolean acquiredLock = dto.hasAcquiredLock();
        State finalState = toState(dto.getFinalState());
        return new Operation(processId, processType, startedAt, spaceId, mtaId, user, acquiredLock, finalState);
    }

    public com.sap.cloud.lm.sl.cf.core.dto.serialization.OperationDto toSerializationDto(Operation ongoingOperation) {
        String processId = ongoingOperation.getProcessId();
        String processType = serializeProcessType(ongoingOperation.getProcessType());
        String startedAt = ongoingOperation.getStartedAt();
        String spaceId = ongoingOperation.getSpaceId();
        String mtaId = ongoingOperation.getMtaId();
        String user = ongoingOperation.getUser();
        String state = toString(ongoingOperation.getState());
        boolean acquiredLock = ongoingOperation.isAcquiredLock();
        return new com.sap.cloud.lm.sl.cf.core.dto.serialization.OperationDto(processId, processType, startedAt, spaceId, mtaId,
            user, acquiredLock, state);
    }

    public com.sap.cloud.lm.sl.cf.core.dto.persistence.OperationDto toPersistenceDto(Operation ongoingOperation) {
        String processId = ongoingOperation.getProcessId();
        String processType = toString(ongoingOperation.getProcessType());
        String startedAt = ongoingOperation.getStartedAt();
        String spaceId = ongoingOperation.getSpaceId();
        String mtaId = ongoingOperation.getMtaId();
        String user = ongoingOperation.getUser();
        String state = toString(ongoingOperation.getState());
        boolean acquiredLock = ongoingOperation.isAcquiredLock();
        return new com.sap.cloud.lm.sl.cf.core.dto.persistence.OperationDto(processId, processType, startedAt, spaceId, mtaId, user,
            acquiredLock, state);
    }

    protected State toState(String operationState) {
        return operationState == null ? null : State.valueOf(operationState);
    }

    protected String toString(State operationState) {
        return operationState == null ? null : operationState.toString();
    }

    protected String serializeProcessType(ProcessType processType) {
        if (processType == null) {
            return null;
        }
        if (processType.equals(ProcessType.DEPLOY)) {
            return DEPLOY_SERIALIZED_NAME;
        }
        if (processType.equals(ProcessType.BLUE_GREEN_DEPLOY)) {
            return BLUE_GREEN_DEPLOY_SERIALIZED_NAME;
        }
        if (processType.equals(ProcessType.UNDEPLOY)) {
            return UNDEPLOY_SERIALIZED_NAME;
        }
        throw new IllegalStateException(MessageFormat.format(Messages.ILLEGAL_PROCESS_TYPE, processType.toString()));
    }

    protected ProcessType deserializeProcessType(String processType) {
        if (processType == null) {
            return null;
        }
        switch (processType) {
            case DEPLOY_SERIALIZED_NAME:
                return ProcessType.DEPLOY;
            case BLUE_GREEN_DEPLOY_SERIALIZED_NAME:
                return ProcessType.BLUE_GREEN_DEPLOY;
            case UNDEPLOY_SERIALIZED_NAME:
                return ProcessType.UNDEPLOY;
            default:
                throw new IllegalStateException(MessageFormat.format(Messages.ILLEGAL_PROCESS_TYPE, processType));
        }
    }

    protected ProcessType toProcessType(String processType) {
        return processType == null ? null : ProcessType.fromString(processType);
    }

    protected String toString(ProcessType processType) {
        return processType == null ? null : processType.toString();
    }

}
