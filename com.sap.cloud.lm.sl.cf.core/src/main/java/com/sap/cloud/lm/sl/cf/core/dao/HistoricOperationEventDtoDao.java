package com.sap.cloud.lm.sl.cf.core.dao;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.DtoWithPrimaryKey;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.HistoricOperationEventDto;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.NamedQueries;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;

@Component
public class HistoricOperationEventDtoDao extends AbstractDtoDao<HistoricOperationEventDto, Long> {

    @Inject
    public HistoricOperationEventDtoDao(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    public int removeBy(String processId) {
        return executeInTransaction(manager -> manager.createNamedQuery(NamedQueries.DELETE_HISTORIC_OPERATION_EVENTS_BY_PROCESS_ID)
            .setParameter(HistoricOperationEventDto.FieldNames.PROCESS_ID, processId)
            .executeUpdate());
    }

    public int removeOlderThan(Date timestamp) {
        return executeInTransaction(manager -> manager.createNamedQuery(NamedQueries.DELETE_HISTORIC_OPERATION_EVENTS_OLDER_THAN)
            .setParameter(HistoricOperationEventDto.FieldNames.TIMESTAMP, timestamp)
            .executeUpdate());
    }

    @SuppressWarnings("unchecked")
    public List<HistoricOperationEventDto> find(String processId) {
        return executeInTransaction(manager -> manager.createNamedQuery(NamedQueries.FIND_HISTORIC_OPERATION_EVENTS_BY_PROCESS_ID)
            .setParameter(HistoricOperationEventDto.FieldNames.PROCESS_ID, processId)
            .getResultList());
    }

    @Override
    protected HistoricOperationEventDto merge(HistoricOperationEventDto existingHistoricOperationEvent, HistoricOperationEventDto delta) {
        long id = existingHistoricOperationEvent.getPrimaryKey();
        String processId = ObjectUtils.firstNonNull(delta.getProcessId(), existingHistoricOperationEvent.getProcessId());
        String type = ObjectUtils.firstNonNull(delta.getType(), existingHistoricOperationEvent.getType());
        Date timestamp = ObjectUtils.firstNonNull(delta.getTimestamp(), existingHistoricOperationEvent.getTimestamp());
        return new HistoricOperationEventDto(id, processId, type, timestamp);
    }

    @Override
    protected void onEntityNotFound(Long id) {
        throw new NotFoundException(Messages.HISTORIC_OPERATION_EVENT_NOT_FOUND, id);
    }

    @Override
    protected void onEntityConflict(HistoricOperationEventDto dto, Throwable t) {
        throw (ConflictException) new ConflictException(Messages.HISTORIC_OPERATION_EVENT_ALREADY_EXISTS, dto.getProcessId(),
            dto.getPrimaryKey()).initCause(t);
    }

    @Override
    protected String getFindAllNamedQuery() {
        return NamedQueries.FIND_ALL_HISTORIC_OPERATION_EVENTS;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <C extends DtoWithPrimaryKey<Long>> Class<C> getDtoClass() {
        return (Class<C>) HistoricOperationEventDto.class;
    }

}
