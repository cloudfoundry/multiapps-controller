package com.sap.cloud.lm.sl.cf.process.jobs;

import static java.text.MessageFormat.format;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import com.sap.cloud.lm.sl.cf.core.persistence.service.HistoricOperationEventService;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Named
@Order(20)
public class HistoricOperationEventsCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(HistoricOperationEventsCleaner.class);

    private HistoricOperationEventService historicOperationEventService;

    @Inject
    public HistoricOperationEventsCleaner(HistoricOperationEventService historicOperationEventService) {
        this.historicOperationEventService = historicOperationEventService;
    }

    @Override
    public void execute(Date expirationTime) {
        LOGGER.debug(CleanUpJob.LOG_MARKER, format(Messages.DELETING_HISTORIC_OPERATION_EVENTS_STORED_BEFORE_0, expirationTime));
        int removedHistoricOperationEvents = historicOperationEventService.createQuery()
                                                                          .olderThan(expirationTime)
                                                                          .delete();
        LOGGER.info(CleanUpJob.LOG_MARKER, format(Messages.DELETED_HISTORIC_OPERATION_EVENTS_0, removedHistoricOperationEvents));
    }

}