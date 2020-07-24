package com.sap.cloud.lm.sl.cf.core.persistence.service;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;

import org.apache.commons.lang3.ObjectUtils;
import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.springframework.context.annotation.Primary;

import com.sap.cloud.lm.sl.cf.core.Messages;
import com.sap.cloud.lm.sl.cf.core.persistence.dto.ProgressMessageDto;
import com.sap.cloud.lm.sl.cf.core.persistence.query.ProgressMessageQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.query.impl.ProgressMessageQueryImpl;
import com.sap.cloud.lm.sl.cf.persistence.model.ImmutableProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage.ProgressMessageType;

@Named
public class ProgressMessageService extends PersistenceService<ProgressMessage, ProgressMessageDto, Long> {

    @Inject
    protected ProgressMessageMapper progressMessageMapper;

    @Inject
    public ProgressMessageService(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    public ProgressMessageQuery createQuery() {
        return new ProgressMessageQueryImpl(createEntityManager(), progressMessageMapper);
    }

    @Override
    protected ProgressMessageDto merge(ProgressMessageDto existingProgressMessage, ProgressMessageDto newProgressMessage) {
        super.merge(existingProgressMessage, newProgressMessage);
        String processId = ObjectUtils.firstNonNull(newProgressMessage.getProcessId(), existingProgressMessage.getProcessId());
        String taskId = ObjectUtils.firstNonNull(newProgressMessage.getTaskId(), existingProgressMessage.getTaskId());
        String type = ObjectUtils.firstNonNull(newProgressMessage.getType(), existingProgressMessage.getType());
        String text = ObjectUtils.firstNonNull(newProgressMessage.getText(), existingProgressMessage.getText());
        Date timestamp = ObjectUtils.firstNonNull(newProgressMessage.getTimestamp(), existingProgressMessage.getTimestamp());
        return getProgressMessageDto(newProgressMessage.getPrimaryKey(), processId, taskId, type, text, timestamp);
    }

    protected ProgressMessageDto getProgressMessageDto(long id, String processId, String taskId, String type, String text, Date timestamp) {
        return new ProgressMessageDto(id, processId, taskId, type, text, timestamp);
    }

    @Override
    protected PersistenceObjectMapper<ProgressMessage, ProgressMessageDto> getPersistenceObjectMapper() {
        return progressMessageMapper;
    }

    @Override
    protected void onEntityConflict(ProgressMessageDto progressMessage, Throwable t) {
        throw new ConflictException(t,
                                    Messages.PROGRESS_MESSAGE_ALREADY_EXISTS,
                                    progressMessage.getProcessId(),
                                    progressMessage.getPrimaryKey());
    }

    @Override
    protected void onEntityNotFound(Long id) {
        throw new NotFoundException(Messages.PROGRESS_MESSAGE_NOT_FOUND, id);
    }

    @Primary
    @Named
    public static class ProgressMessageMapper implements PersistenceObjectMapper<ProgressMessage, ProgressMessageDto> {

        @Override
        public ProgressMessage fromDto(ProgressMessageDto dto) {
            return ImmutableProgressMessage.builder()
                                           .id(dto.getPrimaryKey())
                                           .processId(dto.getProcessId())
                                           .taskId(dto.getTaskId())
                                           .type(getParsedType(dto.getType()))
                                           .text(dto.getText())
                                           .timestamp(dto.getTimestamp())
                                           .build();
        }

        private ProgressMessageType getParsedType(String type) {
            return type == null ? null : ProgressMessageType.valueOf(type);
        }

        @Override
        public ProgressMessageDto toDto(ProgressMessage progressMessage) {
            long id = progressMessage.getId();
            String processId = progressMessage.getProcessId();
            String taskId = progressMessage.getTaskId();
            String type = progressMessage.getType() != null ? progressMessage.getType()
                                                                             .name()
                : null;
            String text = progressMessage.getText();
            Date timestamp = progressMessage.getTimestamp();
            return new ProgressMessageDto(id, processId, taskId, type, text, timestamp);
        }

    }
}