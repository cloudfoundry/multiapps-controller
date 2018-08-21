package com.sap.cloud.lm.sl.cf.process.jobs;

import static java.text.MessageFormat.format;

import java.util.Date;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.persistence.services.ProgressMessageService;

@Component
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
        LOGGER.info(format("Removing progress messages older than \"{0}\"...", expirationTime));
        int removedProcessMessages = progressMessageService.removeOlderThan(expirationTime);
        LOGGER.info(format("Removed progress messages: {0}", removedProcessMessages));
    }

}
