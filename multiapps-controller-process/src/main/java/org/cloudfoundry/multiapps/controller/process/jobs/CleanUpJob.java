package org.cloudfoundry.multiapps.controller.process.jobs;

import static java.text.MessageFormat.format;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.SafeExecutor;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.scheduling.annotation.Scheduled;

@Named
public class CleanUpJob {

    public static final Marker LOG_MARKER = MarkerFactory.getMarker("clean-up-job");
    private static final Logger LOGGER = LoggerFactory.getLogger(CleanUpJob.class);
    private static final int SELECTED_INSTANCE_FOR_CLEAN_UP = 0;

    @Inject
    ApplicationConfiguration configuration;
    @Inject
    List<Cleaner> cleaners;
    private final SafeExecutor safeExecutor = new SafeExecutor(CleanUpJob::log);

    @Scheduled(cron = "#{@applicationConfiguration.getCronExpressionForOldData()}")
    public void execute() {
        if (configuration.getApplicationInstanceIndex() != SELECTED_INSTANCE_FOR_CLEAN_UP) {
            return;
        }
        Instant cleanUpJobStartTime = Instant.now();
        LOGGER.info(LOG_MARKER, format(Messages.CLEAN_UP_JOB_STARTED_BY_APPLICATION_INSTANCE_0_AT_1,
                                       configuration.getApplicationInstanceIndex(), cleanUpJobStartTime));

        LocalDateTime expirationTime = computeExpirationTime();
        LOGGER.info(LOG_MARKER, format(Messages.WILL_CLEAN_UP_DATA_STORED_BEFORE_0, expirationTime));
        LOGGER.info(LOG_MARKER, format(Messages.REGISTERED_CLEANERS_IN_CLEAN_UP_JOB_0, cleaners));
        for (Cleaner cleaner : cleaners) {
            safeExecutor.execute(() -> cleaner.execute(expirationTime));
        }

        LOGGER.info(LOG_MARKER, format(Messages.CLEAN_UP_JOB_WHICH_STARTED_AT_0_HAS_FINISHED_AT_1, cleanUpJobStartTime, Instant.now()));
    }

    private LocalDateTime computeExpirationTime() {
        long maxTtlForOldData = configuration.getMaxTtlForOldData();
        return LocalDateTime.now()
                            .minusSeconds(maxTtlForOldData);
    }

    private static void log(Exception e) {
        LOGGER.error(LOG_MARKER, format(Messages.ERROR_DURING_CLEAN_UP_0, e.getMessage()), e);
    }
}
