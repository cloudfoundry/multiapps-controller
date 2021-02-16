package org.cloudfoundry.multiapps.controller.process.jobs;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

@Named
@Order(5)
public class AbortedOperationsCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbortedOperationsCleaner.class);

    private HistoricOperationEventService historicOperationEventService;
    private FlowableFacade flowableFacade;
    private ApplicationConfiguration applicationConfiguration;

    @Inject
    public AbortedOperationsCleaner(HistoricOperationEventService historicOperationEventService, FlowableFacade flowableFacade,
                                    ApplicationConfiguration applicationConfiguration) {
        this.historicOperationEventService = historicOperationEventService;
        this.flowableFacade = flowableFacade;
        this.applicationConfiguration = applicationConfiguration;
    }

    @Override
    public void execute(Date expirationTime) {
        Instant instant = Instant.now()
                                 .minus(applicationConfiguration.getAbortedOperationsTtlInSeconds(), ChronoUnit.SECONDS);
        LOGGER.debug(CleanUpJob.LOG_MARKER, MessageFormat.format(Messages.DELETING_OPERATIONS_ABORTED_BEFORE_0, instant));
        List<HistoricOperationEvent> abortedOperations = historicOperationEventService.createQuery()
                                                                                      .type(HistoricOperationEvent.EventType.ABORTED)
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
