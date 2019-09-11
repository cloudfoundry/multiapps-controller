package com.sap.cloud.lm.sl.cf.core.persistence.service;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent.EventType;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableHistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.persistence.dto.HistoricOperationEventDto;
import com.sap.cloud.lm.sl.cf.core.persistence.query.HistoricOperationEventQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.query.impl.HistoricOperationEventQueryImpl;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;

@Named
public class HistoricOperationEventService extends PersistenceService<HistoricOperationEvent, HistoricOperationEventDto, Long> {

    @Inject
    private HistoricOperationEventMapper historicOperationEventMapper;

    @Inject
    public HistoricOperationEventService(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    public HistoricOperationEventQuery createQuery() {
        return new HistoricOperationEventQueryImpl(createEntityManager(), historicOperationEventMapper);
    }

    @Override
    protected PersistenceObjectMapper<HistoricOperationEvent, HistoricOperationEventDto> getPersistenceObjectMapper() {
        return historicOperationEventMapper;
    }

    @Override
    protected void onEntityConflict(HistoricOperationEventDto dto, Throwable t) {
        throw new ConflictException(t, Messages.HISTORIC_OPERATION_EVENT_ALREADY_EXISTS, dto.getProcessId(), dto.getPrimaryKey());
    }

    @Override
    protected void onEntityNotFound(Long id) {
        throw new NotFoundException(Messages.HISTORIC_OPERATION_EVENT_NOT_FOUND, id);
    }

    @Named
    public static class HistoricOperationEventMapper implements PersistenceObjectMapper<HistoricOperationEvent, HistoricOperationEventDto> {

        @Override
        public HistoricOperationEvent fromDto(HistoricOperationEventDto dto) {
            return ImmutableHistoricOperationEvent.builder()
                                                  .id(dto.getPrimaryKey())
                                                  .processId(dto.getProcessId())
                                                  .type(getType(dto.getType()))
                                                  .timestamp(dto.getTimestamp())
                                                  .build();
        }

        private EventType getType(String type) {
            return EventType.valueOf(type);
        }

        @Override
        public HistoricOperationEventDto toDto(HistoricOperationEvent historicOperationEvent) {
            long id = historicOperationEvent.getId();
            String processId = historicOperationEvent.getProcessId();
            String type = historicOperationEvent.getType()
                                                .name();
            Date timestamp = historicOperationEvent.getTimestamp();
            return new HistoricOperationEventDto(id, processId, type, timestamp);
        }

    }
}