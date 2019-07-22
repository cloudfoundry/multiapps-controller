package com.sap.cloud.lm.sl.cf.process.util;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.HistoricOperationEventDao;
import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent.EventType;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableHistoricOperationEvent;
import com.sap.cloud.lm.sl.common.ContentException;

@Component
public class HistoricOperationEventPersister {

    @Inject
    private HistoricOperationEventDao dao;

    public void add(String operationId, EventType type) {
        HistoricOperationEvent historicalOperationStateDetails = ImmutableHistoricOperationEvent.builder()
            .processId(operationId)
            .type(type)
            .build();
        dao.add(historicalOperationStateDetails);
    }

    public void add(String operationId, Throwable exception) {
        EventType type = (exception instanceof ContentException) ? EventType.FAILED_BY_CONTENT_ERROR
            : EventType.FAILED_BY_INFRASTRUCTURE_ERROR;
        add(operationId, type);
    }
}
