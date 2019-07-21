package com.sap.cloud.lm.sl.cf.core.dao;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.DtoWithPrimaryKey;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.ProgressMessageDto;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.NamedQueries;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;

@Component("progressMessageDtoDao")
public class ProgressMessageDtoDao extends AbstractDtoDao<ProgressMessageDto, Long> {

    @Inject
    public ProgressMessageDtoDao(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    public int removeBy(String processId) {
        return executeInTransaction(manager -> manager.createNamedQuery(getDeleteByProcessIdQuery())
            .setParameter(1, processId)
            .executeUpdate());
    }

    protected String getDeleteByProcessIdQuery() {
        return NamedQueries.DELETE_PROGRESS_MESSAGES_BY_PROCESS_ID;
    }

    public int removeOlderThan(Date timestamp) {
        return executeInTransaction(manager -> manager.createNamedQuery(getDeleteOlderThanQuery())
            .setParameter(1, timestamp)
            .executeUpdate());
    }

    protected String getDeleteOlderThanQuery() {
        return NamedQueries.DELETE_PROGRESS_MESSAGES_OLDER_THAN;
    }

    public int removeBy(String processId, String taskId, String type) {
        return executeInTransaction(manager -> manager.createNamedQuery(getDeleteByProcessIdTaskIdAndTypeQuery())
            .setParameter(1, processId)
            .setParameter(2, taskId)
            .setParameter(3, type)
            .executeUpdate());
    }

    protected String getDeleteByProcessIdTaskIdAndTypeQuery() {
        return NamedQueries.DELETE_PROGRESS_MESSAGES_BY_PROCESS_AND_TASK_ID_AND_TYPE;
    }

    @SuppressWarnings("unchecked")
    public List<ProgressMessageDto> find(String processId) {
        return execute(manager -> manager.createNamedQuery(getFindByProcessId())
            .setParameter(1, processId)
            .getResultList());
    }

    protected String getFindByProcessId() {
        return NamedQueries.FIND_PROGRESS_MESSAGES_BY_PROCESS_ID;
    }

    @Override
    protected ProgressMessageDto merge(ProgressMessageDto existingProgressMessage, ProgressMessageDto delta) {
        long id = existingProgressMessage.getPrimaryKey();
        String processId = ObjectUtils.firstNonNull(delta.getProcessId(), existingProgressMessage.getProcessId());
        String taskId = ObjectUtils.firstNonNull(delta.getTaskId(), existingProgressMessage.getTaskId());
        String type = ObjectUtils.firstNonNull(delta.getType(), existingProgressMessage.getType());
        String text = ObjectUtils.firstNonNull(delta.getText(), existingProgressMessage.getText());
        Date timestamp = ObjectUtils.firstNonNull(delta.getTimestamp(), existingProgressMessage.getTimestamp());
        return toProgressMessageDto(id, processId, taskId, type, text, timestamp);
    }

    protected ProgressMessageDto toProgressMessageDto(long id, String processId, String taskId, String type, String text, Date timestamp) {
        return new ProgressMessageDto(id, processId, taskId, type, text, timestamp);
    }

    @Override
    protected void onEntityNotFound(Long id) {
        throw new NotFoundException(Messages.PROGRESS_MESSAGE_NOT_FOUND, id);
    }

    @Override
    protected void onEntityConflict(ProgressMessageDto progressMessage, Throwable t) {
        throw (ConflictException) new ConflictException(Messages.PROGRESS_MESSAGE_ALREADY_EXISTS, progressMessage.getProcessId(),
            progressMessage.getPrimaryKey()).initCause(t);
    }

    @Override
    protected String getFindAllNamedQuery() {
        return NamedQueries.FIND_ALL_PROGRESS_MESSAGES;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <C extends DtoWithPrimaryKey<Long>> Class<C> getDtoClass() {
        return (Class<C>) ProgressMessageDto.class;
    }

}
