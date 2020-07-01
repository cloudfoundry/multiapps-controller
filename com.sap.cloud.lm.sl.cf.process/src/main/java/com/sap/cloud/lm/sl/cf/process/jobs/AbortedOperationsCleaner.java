package com.sap.cloud.lm.sl.cf.process.jobs;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent.EventType;
import com.sap.cloud.lm.sl.cf.core.persistence.service.HistoricOperationEventService;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

@Named
@Order(5)
public class AbortedOperationsCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbortedOperationsCleaner.class);

    private HistoricOperationEventService historicOperationEventService;
    private FlowableFacade flowableFacade;

    @Inject
    public AbortedOperationsCleaner(HistoricOperationEventService historicOperationEventService, FlowableFacade flowableFacade) {
        this.historicOperationEventService = historicOperationEventService;
        this.flowableFacade = flowableFacade;
    }

    @Override
    public void execute(Date expirationTime) {
        Instant instant = Instant.now()
                                 .minus(30, ChronoUnit.MINUTES);
        List<HistoricOperationEvent> abortedOperations = historicOperationEventService.createQuery()
                                                                                      .type(EventType.ABORTED)
                                                                                      .olderThan(new Date(instant.toEpochMilli()))
                                                                                      .list();
        abortedOperations.stream()
                         .map(HistoricOperationEvent::getProcessId)
                         .distinct()
                         .filter(this::isInActiveState)
                         .forEach(this::deleteProcessInstance);
    }

    private boolean isInActiveState(String processId) {
        return flowableFacade.getProcessInstance(processId) != null;
    }

    private void deleteProcessInstance(String processInstanceId) {
        try {
            LOGGER.info(CleanUpJob.LOG_MARKER, MessageFormat.format(Messages.DELETING_OPERATION_WITH_ID, processInstanceId));
            flowableFacade.deleteProcessInstance(processInstanceId, Operation.State.ABORTED.name());
        } catch (Exception e) {
            LOGGER.error(CleanUpJob.LOG_MARKER, MessageFormat.format(Messages.ERROR_DELETING_OPERATION_WITH_ID, processInstanceId), e);
        }
    }
}
