package org.cloudfoundry.multiapps.controller.core.application.health.database;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.cloudfoundry.multiapps.controller.core.model.CachedObject;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationInstanceNameUtil;
import org.cloudfoundry.multiapps.controller.persistence.services.DatabaseMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public class DatabaseWaitingLocksAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseWaitingLocksAnalyzer.class);

    private static final int POLLING_LOCKS_INTERVAL_IN_SECONDS = 10;
    private static final Duration MAXIMUM_VALIDITY_OF_LOCKS_SAMPLE_IN_MINUTES = Duration.ofMinutes(6);
    private static final Duration ANOMALY_DETECTION_THRESHOLD_IN_MINUTES = Duration.ofMinutes(5);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final List<CachedObject<Long>> waitingLocksSamples = new LinkedList<>();

    private final DatabaseMonitoringService databaseMonitoringService;
    private final ApplicationConfiguration applicationConfiguration;

    @Inject
    public DatabaseWaitingLocksAnalyzer(DatabaseMonitoringService databaseMonitoringService,
                                        ApplicationConfiguration applicationConfiguration) {
        this.databaseMonitoringService = databaseMonitoringService;
        this.applicationConfiguration = applicationConfiguration;
        executor.scheduleAtFixedRate(this::refreshLockInfo, 0, POLLING_LOCKS_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
    }

    private void refreshLockInfo() {
        deleteObsoleteSamples();
        takeLocksSample();
    }

    private void deleteObsoleteSamples() {
        waitingLocksSamples.removeIf(CachedObject::isExpired);
    }

    private void takeLocksSample() {
        waitingLocksSamples.add(new CachedObject<>(databaseMonitoringService.getProcessesWaitingForLocks(ApplicationInstanceNameUtil.buildApplicationInstanceTemplate(applicationConfiguration)),
                                                   System.currentTimeMillis() + MAXIMUM_VALIDITY_OF_LOCKS_SAMPLE_IN_MINUTES.toMillis()));
    }

    public boolean hasIncreasedDbLocks() {
        long minimumRequiredSamplesCount = ANOMALY_DETECTION_THRESHOLD_IN_MINUTES.toSeconds() / POLLING_LOCKS_INTERVAL_IN_SECONDS;
        if (waitingLocksSamples.size() < minimumRequiredSamplesCount) {
            LOGGER.info(MessageFormat.format("Not enough samples to detect anomaly: {0} / {1}", waitingLocksSamples.size(), // make debug
                                             minimumRequiredSamplesCount));
            return false;
        }
        LOGGER.info("Values in the waiting for locks samples: {}", waitingLocksSamples.stream() // make debug
                                                                                      .map(CachedObject::get)
                                                                                      .toList());
        return calculateIncreasingOrEqualIndex() >= 0.6;
    }

    public double calculateIncreasingOrEqualIndex() {
        int increasingOrEqualCount = 0;
        int decreasingCount = 0;
        int totalComparisons = waitingLocksSamples.size() - 1;
        if (waitingLocksSamples.get(0)
                               .get() < 5) {
            return 0;
        }
        for (int i = 1; i < waitingLocksSamples.size(); i++) {
            long currentLocksCount = waitingLocksSamples.get(i)
                                                        .get();
            long previousLocksCount = waitingLocksSamples.get(i - 1)
                                                         .get();
            if (currentLocksCount < 5) {
                return 0;
            }
            if (currentLocksCount >= previousLocksCount) {
                increasingOrEqualCount++;
            } else {
                decreasingCount++;
            }
        }
        double increasingOrEqualIndex = (double) increasingOrEqualCount / totalComparisons;
        double decreasingIndex = (double) decreasingCount / totalComparisons;
        LOGGER.info(MessageFormat.format("Increasing or equal index: {0} / {1} = {2}", increasingOrEqualCount, totalComparisons,
                                         increasingOrEqualIndex));
        LOGGER.info(MessageFormat.format("Decreasing index: {0} / {1} = {2}", decreasingCount, totalComparisons, decreasingIndex));
        return increasingOrEqualIndex;
    }
}
