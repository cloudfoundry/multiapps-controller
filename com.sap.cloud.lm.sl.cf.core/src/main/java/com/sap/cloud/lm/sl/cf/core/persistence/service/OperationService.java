package com.sap.cloud.lm.sl.cf.core.persistence.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;

import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.NotFoundException;

import com.sap.cloud.lm.sl.cf.core.Messages;
import com.sap.cloud.lm.sl.cf.core.persistence.dto.OperationDto;
import com.sap.cloud.lm.sl.cf.core.persistence.query.OperationQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.query.impl.OperationQueryImpl;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableOperation;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;

@Named
public class OperationService extends PersistenceService<Operation, OperationDto, String> {

    @Inject
    protected OperationMapper operationMapper;

    @Inject
    public OperationService(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    public OperationQuery createQuery() {
        return new OperationQueryImpl(createEntityManager(), operationMapper);
    }

    @Override
    protected PersistenceObjectMapper<Operation, OperationDto> getPersistenceObjectMapper() {
        return operationMapper;
    }

    @Override
    protected void onEntityNotFound(String processId) {
        throw new NotFoundException(Messages.OPERATION_NOT_FOUND, processId);
    }

    @Override
    protected void onEntityConflict(OperationDto dto, Throwable t) {
        String processId = dto.getPrimaryKey();
        throw new ConflictException(t, Messages.OPERATION_ALREADY_EXISTS, processId);
    }

    @Named
    public static class OperationMapper implements PersistenceObjectMapper<Operation, OperationDto> {

        @Override
        public Operation fromDto(OperationDto dto) {
            return ImmutableOperation.builder()
                                     .processId(dto.getPrimaryKey())
                                     .processType(toProcessType(dto.getProcessType()))
                                     .startedAt(toZonedDateTime(dto.getStartedAt()))
                                     .endedAt(toZonedDateTime(dto.getEndedAt()))
                                     .spaceId(dto.getSpaceId())
                                     .mtaId(dto.getMtaId())
                                     .namespace(dto.getNamespace())
                                     .user(dto.getUser())
                                     .hasAcquiredLock(dto.hasAcquiredLock())
                                     .state(toState(dto.getFinalState()))
                                     .build();
        }

        private ProcessType toProcessType(String processType) {
            return processType == null ? null : ProcessType.fromString(processType);
        }

        private Operation.State toState(String operationState) {
            return operationState == null ? null : Operation.State.valueOf(operationState);
        }

        private ZonedDateTime toZonedDateTime(Date date) {
            return date == null ? null : ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));
        }

        @Override
        public OperationDto toDto(Operation operation) {
            String processId = operation.getProcessId();
            String processType = toString(operation.getProcessType());
            Date startedAt = toDate(operation.getStartedAt());
            Date endedAt = toDate(operation.getEndedAt());
            String spaceId = operation.getSpaceId();
            String mtaId = operation.getMtaId();
            String namespace = operation.getNamespace();
            String user = operation.getUser();
            String state = toString(operation.getState());
            boolean acquiredLock = operation.hasAcquiredLock();
            return OperationDto.builder()
                               .processId(processId)
                               .processType(processType)
                               .startedAt(startedAt)
                               .endedAt(endedAt)
                               .spaceId(spaceId)
                               .mtaId(mtaId)
                               .namespace(namespace)
                               .user(user)
                               .acquiredLock(acquiredLock)
                               .finalState(state)
                               .build();
        }

        private Date toDate(ZonedDateTime zonedDateTime) {
            return zonedDateTime == null ? null
                : new Date(zonedDateTime.toInstant()
                                        .toEpochMilli());
        }

        private String toString(ProcessType processType) {
            return processType == null ? null : processType.toString();
        }

        private String toString(Operation.State operationState) {
            return operationState == null ? null : operationState.toString();
        }

    }
}