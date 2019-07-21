package com.sap.cloud.lm.sl.cf.core.dao;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.ProgressMessageDto;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage.ProgressMessageType;

@Component("progressMessageDao")
public class ProgressMessageDao extends AbstractDao<ProgressMessage, ProgressMessageDto, Long> {

    @Inject
    protected ProgressMessageDtoDao progressMessageDtoDao;

    public int removeBy(String processId) {
        return getDtoDao().removeBy(processId);
    }

    public int removeOlderThan(Date timestamp) {
        return getDtoDao().removeOlderThan(timestamp);
    }

    public int removeBy(String processId, String taskId, ProgressMessageType type) {
        return getDtoDao().removeBy(processId, taskId, type.name());
    }

    public List<ProgressMessage> find(String processId) {
        return fromDtos(getDtoDao().find(processId));
    }

    @Override
    protected ProgressMessageDtoDao getDtoDao() {
        return progressMessageDtoDao;
    }

    @Override
    protected ProgressMessage fromDto(ProgressMessageDto progressMessageDto) {
        return progressMessageDto.toProgressMessage();
    }

    @Override
    protected ProgressMessageDto toDto(ProgressMessage progressMessage) {
        return new ProgressMessageDto(progressMessage);
    }
}
