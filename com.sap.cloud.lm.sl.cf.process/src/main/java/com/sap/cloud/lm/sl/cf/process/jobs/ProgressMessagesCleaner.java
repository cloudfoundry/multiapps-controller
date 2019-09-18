package com.sap.cloud.lm.sl.cf.process.jobs;

import static java.text.MessageFormat.format;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import com.sap.cloud.lm.sl.cf.core.persistence.service.ProgressMessageService;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

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
    public void execute(Date expirationTime) {
        LOGGER.debug(CleanUpJob.LOG_MARKER, format(Messages.DELETING_PROGRESS_MESSAGES_STORED_BEFORE_0, expirationTime));
        int removedProgressMessages = progressMessageService.createQuery()
                                                            .olderThan(expirationTime)
                                                            .delete();
        LOGGER.info(CleanUpJob.LOG_MARKER, format(Messages.DELETED_PROGRESS_MESSAGES_0, removedProgressMessages));
    }

}
