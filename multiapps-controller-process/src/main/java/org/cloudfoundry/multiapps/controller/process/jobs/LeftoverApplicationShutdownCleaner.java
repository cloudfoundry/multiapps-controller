package org.cloudfoundry.multiapps.controller.process.jobs;

import java.text.MessageFormat;
import java.time.LocalDateTime;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.persistence.services.ApplicationShutdownService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class LeftoverApplicationShutdownCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeftoverApplicationShutdownCleaner.class);
    private static final int ONE_DAY_IN_SECONDS = 86400;

    private final ApplicationShutdownService applicationShutdownService;

    public LeftoverApplicationShutdownCleaner(ApplicationShutdownService applicationShutdownService) {
        this.applicationShutdownService = applicationShutdownService;
    }

    @Override
    public void execute(LocalDateTime expirationTime) {
        LocalDateTime timeNow = LocalDateTime.now();
        LocalDateTime secondsAfterStartedDate = timeNow.minusSeconds(ONE_DAY_IN_SECONDS);

        int countOfDeletedApplicationShutdowns = applicationShutdownService.createQuery()
                                                                           .startedAtBefore(secondsAfterStartedDate)
                                                                           .delete();

        LOGGER.info(MessageFormat.format(Messages.DELETED_LEFTOVER_APPLICATION_SHUTDOWNS, countOfDeletedApplicationShutdowns));
    }
}
