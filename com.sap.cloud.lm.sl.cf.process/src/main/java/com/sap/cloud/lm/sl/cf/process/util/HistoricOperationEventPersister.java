package com.sap.cloud.lm.sl.cf.process.util;

import javax.inject.Inject;
import javax.inject.Named;

import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent.EventType;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableHistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.persistence.service.HistoricOperationEventService;
import com.sap.cloud.lm.sl.common.ContentException;

@Named
public class HistoricOperationEventPersister {

    @Inject
    private HistoricOperationEventService configurationSubscriptionService;

    public void add(String operationId, EventType type) {
        HistoricOperationEvent historicalOperationStateDetails = ImmutableHistoricOperationEvent.builder()
                                                                                                .processId(operationId)
                                                                                                .type(type)
                                                                                                .build();
        configurationSubscriptionService.add(historicalOperationStateDetails);
    }

    public void add(String operationId, Throwable exception) {
        EventType type = (exception instanceof ContentException) ? EventType.FAILED_BY_CONTENT_ERROR
            : EventType.FAILED_BY_INFRASTRUCTURE_ERROR;
        add(operationId, type);
    }
}