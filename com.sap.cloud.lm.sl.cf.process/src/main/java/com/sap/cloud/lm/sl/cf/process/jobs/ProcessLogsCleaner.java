package com.sap.cloud.lm.sl.cf.process.jobs;

import static java.text.MessageFormat.format;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLogsPersistenceService;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Named
@Order(20)
public class ProcessLogsCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessLogsCleaner.class);

    private final ProcessLogsPersistenceService processLogsPersistenceService;

    @Inject
    public ProcessLogsCleaner(ProcessLogsPersistenceService processLogsPersistenceService) {
        this.processLogsPersistenceService = processLogsPersistenceService;
    }

    @Override
    public void execute(Date expirationTime) {
        LOGGER.debug(CleanUpJob.LOG_MARKER, format(Messages.DELETING_PROCESS_LOGS_MODIFIED_BEFORE_0, expirationTime));
        try {
            int deletedProcessLogs = processLogsPersistenceService.deleteModifiedBefore(expirationTime);
            LOGGER.info(CleanUpJob.LOG_MARKER, format(Messages.DELETED_PROCESS_LOGS_0, deletedProcessLogs));
        } catch (FileStorageException e) {
            throw new SLException(e, Messages.COULD_NOT_DELETE_PROCESS_LOGS_MODIFIED_BEFORE_0, expirationTime);
        }
    }

}
