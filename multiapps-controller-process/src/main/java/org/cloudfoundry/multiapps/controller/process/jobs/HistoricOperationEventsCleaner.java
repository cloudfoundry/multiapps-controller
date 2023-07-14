package org.cloudfoundry.multiapps.controller.process.jobs;

import static java.text.MessageFormat.format;

import java.time.LocalDateTime;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

@Named
@Order(20)
public class HistoricOperationEventsCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(HistoricOperationEventsCleaner.class);

    private final HistoricOperationEventService historicOperationEventService;

    @Inject
    public HistoricOperationEventsCleaner(HistoricOperationEventService historicOperationEventService) {
        this.historicOperationEventService = historicOperationEventService;
    }

    @Override
    public void execute(LocalDateTime expirationTime) {
        LOGGER.debug(CleanUpJob.LOG_MARKER, format(Messages.DELETING_HISTORIC_OPERATION_EVENTS_STORED_BEFORE_0, expirationTime));
        int removedHistoricOperationEvents = historicOperationEventService.createQuery()
                                                                          .olderThan(expirationTime)
                                                                          .delete();
        LOGGER.info(CleanUpJob.LOG_MARKER, format(Messages.DELETED_HISTORIC_OPERATION_EVENTS_0, removedHistoricOperationEvents));
    }

}