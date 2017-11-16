package com.sap.cloud.lm.sl.cf.core.helpers;

import java.time.ZonedDateTime;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.OperationDto;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.cf.web.api.model.State;

@Component
public class OperationFactory {

    public Operation fromPersistenceDto(OperationDto dto) {
        return new Operation().processId(dto.getProcessId())
            .processType(toProcessType(dto.getProcessType()))
            .startedAt(toZonedDateTime(dto.getStartedAt()))
            .endedAt(toZonedDateTime(dto.getEndedAt()))
            .spaceId(dto.getSpaceId())
            .mtaId(dto.getMtaId())
            .user(dto.getUser())
            .acquiredLock(dto.hasAcquiredLock())
            .state(toState(dto.getFinalState()));
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
        return zonedDateTime == null ? null : ZonedDateTime.parse(zonedDateTime, Operation.DATE_TIME_FORMATTER);
    }

    protected String toString(ZonedDateTime zonedDateTime) {
        return zonedDateTime == null ? null : Operation.DATE_TIME_FORMATTER.format(zonedDateTime);
    }

    protected ProcessType toProcessType(String processType) {
        return processType == null ? null : ProcessType.fromString(processType);
    }

    protected String toString(ProcessType processType) {
        return processType == null ? null : processType.toString();
    }

}
