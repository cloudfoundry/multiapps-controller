package com.sap.cloud.lm.sl.cf.core.helpers;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.OperationDto;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.cf.web.api.model.State;

@Component
public class OperationFactory {

    public Operation fromPersistenceDto(OperationDto dto) {
        return new Operation().processId(dto.getPrimaryKey())
            .processType(toProcessType(dto.getProcessType()))
            .startedAt(toZonedDateTime(dto.getStartedAt()))
            .endedAt(toZonedDateTime(dto.getEndedAt()))
            .spaceId(dto.getSpaceId())
            .mtaId(dto.getMtaId())
            .user(dto.getUser())
            .acquiredLock(dto.hasAcquiredLock())
            .state(toState(dto.getFinalState()));
    }

    public OperationDto toPersistenceDto(Operation operation) {
        String processId = operation.getProcessId();
        String processType = toString(operation.getProcessType());
        Date startedAt = toDate(operation.getStartedAt());
        Date endedAt = toDate(operation.getEndedAt());
        String spaceId = operation.getSpaceId();
        String mtaId = operation.getMtaId();
        String user = operation.getUser();
        String state = toString(operation.getState());
        boolean acquiredLock = operation.hasAcquiredLock();
        return new OperationDto(processId, processType, startedAt, endedAt, spaceId, mtaId, user, acquiredLock, state);
    }

    protected State toState(String operationState) {
        return operationState == null ? null : State.valueOf(operationState);
    }

    protected String toString(State operationState) {
        return operationState == null ? null : operationState.toString();
    }

    protected ZonedDateTime toZonedDateTime(Date date) {
        return date == null ? null : ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));
    }

    protected Date toDate(ZonedDateTime zonedDateTime) {
        return zonedDateTime == null ? null
            : new Date(zonedDateTime.toInstant()
                .toEpochMilli());
    }

    protected ProcessType toProcessType(String processType) {
        return processType == null ? null : ProcessType.fromString(processType);
    }

    protected String toString(ProcessType processType) {
        return processType == null ? null : processType.toString();
    }

}
