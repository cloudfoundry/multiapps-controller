package com.sap.cloud.lm.sl.cf.core.helpers;

import java.text.MessageFormat;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.OngoingOperation;
import com.sap.cloud.lm.sl.cf.core.model.ProcessType;
import com.sap.lmsl.slp.SlpTaskState;

@Component
public class OngoingOperationFactory {

    private static final String DEPLOY_SERIALIZED_NAME = "deploy";
    private static final String BLUE_GREEN_DEPLOY_SERIALIZED_NAME = "blue-green-deploy";
    private static final String UNDEPLOY_SERIALIZED_NAME = "undeploy";

    public OngoingOperation fromSerializationDto(com.sap.cloud.lm.sl.cf.core.dto.serialization.OngoingOperationDto dto) {
        String processId = dto.getProcessId();
        ProcessType processType = deserializeProcessType(dto.getProcessType());
        String startedAt = dto.getStartedAt();
        String spaceId = dto.getSpaceId();
        String mtaId = dto.getMtaId();
        String user = dto.getUser();
        boolean acquiredLock = dto.hasAcquiredLock();
        SlpTaskState finalState = toSlpTaskState(dto.getState());
        return new OngoingOperation(processId, processType, startedAt, spaceId, mtaId, user, acquiredLock, finalState);
    }

    public OngoingOperation fromPersistenceDto(com.sap.cloud.lm.sl.cf.core.dto.persistence.OngoingOperationDto dto) {
        String processId = dto.getProcessId();
        ProcessType processType = toProcessType(dto.getProcessType());
        String startedAt = dto.getStartedAt();
        String spaceId = dto.getSpaceId();
        String mtaId = dto.getMtaId();
        String user = dto.getUser();
        boolean acquiredLock = dto.hasAcquiredLock();
        SlpTaskState finalState = toSlpTaskState(dto.getFinalState());
        return new OngoingOperation(processId, processType, startedAt, spaceId, mtaId, user, acquiredLock, finalState);
    }

    public com.sap.cloud.lm.sl.cf.core.dto.serialization.OngoingOperationDto toSerializationDto(OngoingOperation ongoingOperation) {
        String processId = ongoingOperation.getProcessId();
        String processType = serializeProcessType(ongoingOperation.getProcessType());
        String startedAt = ongoingOperation.getStartedAt();
        String spaceId = ongoingOperation.getSpaceId();
        String mtaId = ongoingOperation.getMtaId();
        String user = ongoingOperation.getUser();
        String state = toString(ongoingOperation.getFinalState());
        boolean acquiredLock = ongoingOperation.hasAcquiredLock();
        return new com.sap.cloud.lm.sl.cf.core.dto.serialization.OngoingOperationDto(processId, processType, startedAt, spaceId, mtaId,
            user, acquiredLock, state);
    }

    public com.sap.cloud.lm.sl.cf.core.dto.persistence.OngoingOperationDto toPersistenceDto(OngoingOperation ongoingOperation) {
        String processId = ongoingOperation.getProcessId();
        String processType = toString(ongoingOperation.getProcessType());
        String startedAt = ongoingOperation.getStartedAt();
        String spaceId = ongoingOperation.getSpaceId();
        String mtaId = ongoingOperation.getMtaId();
        String user = ongoingOperation.getUser();
        String state = toString(ongoingOperation.getFinalState());
        boolean acquiredLock = ongoingOperation.hasAcquiredLock();
        return new com.sap.cloud.lm.sl.cf.core.dto.persistence.OngoingOperationDto(processId, processType, startedAt, spaceId, mtaId, user,
            acquiredLock, state);
    }

    protected SlpTaskState toSlpTaskState(String slpTaskState) {
        return slpTaskState == null ? null : SlpTaskState.valueOf(slpTaskState);
    }

    protected String toString(SlpTaskState slpTaskState) {
        return slpTaskState == null ? null : slpTaskState.toString();
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
