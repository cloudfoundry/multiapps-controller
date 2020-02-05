package com.sap.cloud.lm.sl.cf.process.util;

import javax.inject.Inject;
import javax.inject.Named;

import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent.EventType;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableHistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.persistence.service.HistoricOperationEventService;

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

}