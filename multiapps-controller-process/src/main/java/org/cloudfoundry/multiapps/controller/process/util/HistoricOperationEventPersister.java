package org.cloudfoundry.multiapps.controller.process.util;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.core.model.HistoricOperationEvent.EventType;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableHistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.core.persistence.service.HistoricOperationEventService;

@Named
public class HistoricOperationEventPersister {

    private final HistoricOperationEventService configurationSubscriptionService;

    @Inject
    public HistoricOperationEventPersister(HistoricOperationEventService configurationSubscriptionService) {
        this.configurationSubscriptionService = configurationSubscriptionService;
    }

    public void add(String operationId, EventType type) {
        HistoricOperationEvent historicalOperationStateDetails = ImmutableHistoricOperationEvent.builder()
                                                                                                .processId(operationId)
                                                                                                .type(type)
                                                                                                .build();
        configurationSubscriptionService.add(historicalOperationStateDetails);
    }

}