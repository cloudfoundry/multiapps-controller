package com.sap.cloud.lm.sl.cf.core.helpers;

import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    public Operation fromSerializationDto(com.sap.cloud.lm.sl.cf.core.dto.serialization.OperationDto dto) {
        String processId = dto.getProcessId();
        ProcessType processType = deserializeProcessType(dto.getProcessType());
        ZonedDateTime startedAt = toZonedDateTime(dto.getStartedAt());
        ZonedDateTime endedAt = toZonedDateTime(dto.getEndedAt());
        String spaceId = dto.getSpaceId();
        String mtaId = dto.getMtaId();
        String user = dto.getUser();
        boolean acquiredLock = dto.hasAcquiredLock();
        State finalState = toState(dto.getState());
        return new Operation(processId, processType, startedAt, endedAt, spaceId, mtaId, user, acquiredLock, finalState);
    }

    public Operation fromPersistenceDto(com.sap.cloud.lm.sl.cf.core.dto.persistence.OperationDto dto) {
        String processId = dto.getProcessId();
        ProcessType processType = toProcessType(dto.getProcessType());
        ZonedDateTime startedAt = toZonedDateTime(dto.getStartedAt());
        ZonedDateTime endedAt = toZonedDateTime(dto.getEndedAt());
        String spaceId = dto.getSpaceId();
        String mtaId = dto.getMtaId();
        String user = dto.getUser();
        boolean acquiredLock = dto.hasAcquiredLock();
        State finalState = toState(dto.getFinalState());
        return new Operation(processId, processType, startedAt, endedAt, spaceId, mtaId, user, acquiredLock, finalState);
    }

    public com.sap.cloud.lm.sl.cf.core.dto.serialization.OperationDto toSerializationDto(Operation operation) {
        String processId = operation.getProcessId();
        String processType = serializeProcessType(operation.getProcessType());
        String startedAt = toString(operation.getStartedAt());
        String endedAt = toString(operation.getEndedAt());
        String spaceId = operation.getSpaceId();
        String mtaId = operation.getMtaId();
        String user = operation.getUser();
        String state = toString(operation.getState());
        boolean acquiredLock = operation.hasAcquiredLock();
        return new com.sap.cloud.lm.sl.cf.core.dto.serialization.OperationDto(processId, processType, startedAt, endedAt, spaceId, mtaId,
            user, acquiredLock, state);
    }

    public com.sap.cloud.lm.sl.cf.core.dto.persistence.OperationDto toPersistenceDto(Operation operation) {
        String processId = operation.getProcessId();
        String processType = toString(operation.getProcessType());
        String startedAt = toString(operation.getStartedAt());
        String endedAt = toString(operation.getEndedAt());
        String spaceId = operation.getSpaceId();
        String mtaId = operation.getMtaId();
        String user = operation.getUser();
        String state = toString(operation.getState());
        boolean acquiredLock = operation.hasAcquiredLock();
        return new com.sap.cloud.lm.sl.cf.core.dto.persistence.OperationDto(processId, processType, startedAt, endedAt, spaceId, mtaId,
            user, acquiredLock, state);
    }

    protected State toState(String operationState) {
        return operationState == null ? null : State.valueOf(operationState);
    }

    protected String toString(State operationState) {
        return operationState == null ? null : operationState.toString();
    }
    
    protected ZonedDateTime toZonedDateTime(String zonedDateTime) {
        return zonedDateTime == null ? null : ZonedDateTime.parse(zonedDateTime, FORMATTER);
    }

    protected String toString(ZonedDateTime zonedDateTime) {
        return zonedDateTime == null ? null : FORMATTER.format(zonedDateTime);
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
