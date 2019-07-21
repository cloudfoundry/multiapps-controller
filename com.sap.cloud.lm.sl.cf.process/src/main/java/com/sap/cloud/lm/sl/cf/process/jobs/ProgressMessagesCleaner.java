package com.sap.cloud.lm.sl.cf.process.jobs;

import static java.text.MessageFormat.format;

import java.util.Date;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.ProgressMessageDao;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component
@Order(20)
public class ProgressMessagesCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressMessagesCleaner.class);

    private final ProgressMessageDao progressMessageDao;

    @Inject
    public ProgressMessagesCleaner(ProgressMessageDao progressMessageDao) {
        this.progressMessageDao = progressMessageDao;
    }

    @Override
    public void execute(Date expirationTime) {
        LOGGER.debug(CleanUpJob.LOG_MARKER, format(Messages.DELETING_PROGRESS_MESSAGES_STORED_BEFORE_0, expirationTime));
        int removedProgressMessages = progressMessageDao.removeOlderThan(expirationTime);
        LOGGER.info(CleanUpJob.LOG_MARKER, format(Messages.DELETED_PROGRESS_MESSAGES_0, removedProgressMessages));
    }

}
