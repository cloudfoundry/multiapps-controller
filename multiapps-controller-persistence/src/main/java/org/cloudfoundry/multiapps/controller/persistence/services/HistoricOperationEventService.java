package org.cloudfoundry.multiapps.controller.persistence.services;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;

import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.dto.HistoricOperationEventDto;
import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableHistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.query.HistoricOperationEventQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.impl.HistoricOperationEventQueryImpl;

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

        private HistoricOperationEvent.EventType getType(String type) {
            return HistoricOperationEvent.EventType.valueOf(type);
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