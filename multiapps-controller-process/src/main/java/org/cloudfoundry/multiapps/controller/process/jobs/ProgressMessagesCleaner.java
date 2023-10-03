package org.cloudfoundry.multiapps.controller.process.jobs;

import static java.text.MessageFormat.format;

import java.time.LocalDateTime;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

@Named
@Order(20)
public class ProgressMessagesCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressMessagesCleaner.class);

    private final ProgressMessageService progressMessageService;

    @Inject
    public ProgressMessagesCleaner(ProgressMessageService progressMessageService) {
        this.progressMessageService = progressMessageService;
    }

    @Override
    public void execute(LocalDateTime expirationTime) {
        LOGGER.debug(CleanUpJob.LOG_MARKER, format(Messages.DELETING_PROGRESS_MESSAGES_STORED_BEFORE_0, expirationTime));
        int removedProgressMessages = progressMessageService.createQuery()
                                                            .olderThan(expirationTime)
                                                            .delete();
        LOGGER.info(CleanUpJob.LOG_MARKER, format(Messages.DELETED_PROGRESS_MESSAGES_0, removedProgressMessages));
    }

}
