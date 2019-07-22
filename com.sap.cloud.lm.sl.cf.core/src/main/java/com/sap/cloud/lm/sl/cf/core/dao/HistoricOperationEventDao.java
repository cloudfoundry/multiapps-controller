package com.sap.cloud.lm.sl.cf.core.dao;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.HistoricOperationEventDto;
import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent;

@Component
public class HistoricOperationEventDao
    extends AbstractDao<HistoricOperationEvent, HistoricOperationEventDto, Long> {

    @Inject
    protected HistoricOperationEventDtoDao dtoDao;

    public int removeBy(String processId) {
        return dtoDao.removeBy(processId);
    }

    public int removeOlderThan(Date timestamp) {
        return dtoDao.removeOlderThan(timestamp);
    }

    public List<HistoricOperationEvent> find(String processId) {
        return fromDtos(dtoDao.find(processId));
    }

    @Override
    protected AbstractDtoDao<HistoricOperationEventDto, Long> getDtoDao() {
        return dtoDao;
    }

    @Override
    protected HistoricOperationEvent fromDto(HistoricOperationEventDto historicOperationEventDto) {
        return historicOperationEventDto.toHistoricOperationEvent();
    }

    @Override
    protected HistoricOperationEventDto toDto(HistoricOperationEvent historicOperationEvent) {
        return new HistoricOperationEventDto(historicOperationEvent);
    }
}
