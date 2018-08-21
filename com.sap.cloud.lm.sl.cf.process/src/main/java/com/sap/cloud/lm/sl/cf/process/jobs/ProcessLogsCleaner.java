package com.sap.cloud.lm.sl.cf.process.jobs;

import static java.text.MessageFormat.format;

import java.util.Date;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLogsPersistenceService;
import com.sap.cloud.lm.sl.common.SLException;

@Component
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
        LOGGER.info(format("Deleting process logs older than \"{0}\"...", expirationTime));
        try {
            int deletedProcessLogs = processLogsPersistenceService.deleteByModificationTime(expirationTime);
            LOGGER.info(format("Deleted process logs: {0}", deletedProcessLogs));
        } catch (FileStorageException e) {
            throw new SLException(e, "Deletion of process logs failed");
        }
    }

}
