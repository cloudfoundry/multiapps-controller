package com.sap.cloud.lm.sl.cf.process.jobs;

import static java.text.MessageFormat.format;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;

@DisallowConcurrentExecution
public class CleanUpJob implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanUpJob.class);
    private static final String LOG_ERROR_MESSAGE_PATTERN = "[Clean up Job] Error during cleaning up: {0}";

    @Inject
    ApplicationConfiguration configuration;
    @Inject
    List<Cleaner> cleaners;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOGGER.info(format("Cleanup Job started by application instance: {0}  at: {1}", configuration.getApplicationInstanceIndex(),
            Instant.now()));

        Date expirationTime = computeExpirationTime();
        for (Cleaner cleaner : cleaners) {
            executeSafely(() -> cleaner.execute(expirationTime));
        }

        LOGGER.info(format("Cleanup Job finished at: {0}", Instant.now()));
    }

    private Date computeExpirationTime() {
        long maxTtlForOldData = configuration.getMaxTtlForOldData();
        Date cleanUpTimestamp = Date.from(Instant.now()
            .minusSeconds(maxTtlForOldData));
        LOGGER.info(format("Will perform clean up for data stored before: {0}", cleanUpTimestamp));
        return cleanUpTimestamp;
    }

    private void executeSafely(Runnable r) {
        try {
            r.run();
        } catch (Exception e) {
            LOGGER.error(format(LOG_ERROR_MESSAGE_PATTERN, e.getMessage()), e);
        }
    }

}
