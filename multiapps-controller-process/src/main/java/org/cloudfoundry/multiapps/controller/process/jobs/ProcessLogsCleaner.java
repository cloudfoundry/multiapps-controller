package org.cloudfoundry.multiapps.controller.process.jobs;

import static java.text.MessageFormat.format;

import java.time.LocalDateTime;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogsPersistenceService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

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
    public void execute(LocalDateTime expirationTime) {
        LOGGER.debug(CleanUpJob.LOG_MARKER, format(Messages.DELETING_PROCESS_LOGS_MODIFIED_BEFORE_0, expirationTime));
        try {
            int deletedProcessLogs = processLogsPersistenceService.deleteModifiedBefore(expirationTime);
            LOGGER.info(CleanUpJob.LOG_MARKER, format(Messages.DELETED_PROCESS_LOGS_0, deletedProcessLogs));
        } catch (FileStorageException e) {
            throw new SLException(e, Messages.COULD_NOT_DELETE_PROCESS_LOGS_MODIFIED_BEFORE_0, expirationTime);
        }
    }

}
