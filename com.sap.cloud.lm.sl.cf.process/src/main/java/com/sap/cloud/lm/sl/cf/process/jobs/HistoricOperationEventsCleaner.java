package com.sap.cloud.lm.sl.cf.process.jobs;

import static java.text.MessageFormat.format;

import java.util.Date;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.HistoricOperationEventDao;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component
@Order(20)
public class HistoricOperationEventsCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(HistoricOperationEventsCleaner.class);

    private HistoricOperationEventDao historicOperationEventDao;

    @Inject
    public HistoricOperationEventsCleaner(HistoricOperationEventDao historicOperationEventDao) {
        this.historicOperationEventDao = historicOperationEventDao;
    }

    @Override
    public void execute(Date expirationTime) {
        LOGGER.debug(CleanUpJob.LOG_MARKER, format(Messages.DELETING_HISTORIC_OPERATION_EVENTS_STORED_BEFORE_0, expirationTime));
        int removedHistoricOperationEvents = historicOperationEventDao.removeOlderThan(expirationTime);
        LOGGER.info(CleanUpJob.LOG_MARKER, format(Messages.DELETED_HISTORIC_OPERATION_EVENTS_0, removedHistoricOperationEvents));
    }

}
