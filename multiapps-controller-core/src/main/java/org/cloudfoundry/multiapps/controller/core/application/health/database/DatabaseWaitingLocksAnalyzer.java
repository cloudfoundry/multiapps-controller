package org.cloudfoundry.multiapps.controller.core.application.health.database;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.cloudfoundry.multiapps.controller.core.Messages;
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
    private static final int MAXIMUM_VALUE_OF_NORMAL_LOCKS_COUNT = 5;
    private static final double MAXIMAL_ACCEPTABLE_INCREMENTAL_LOCKS_DEVIATION_INDEX = 0.5;
    private static final int MINIMUM_REQUIRED_INCREASED_SAMPLES_REQUIRED_FOR_LOGGING = 5;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final List<CachedObject<Long>> waitingLocksSamples = new LinkedList<>();

    private final DatabaseMonitoringService databaseMonitoringService;
    private final ApplicationConfiguration applicationConfiguration;

    @Inject
    public DatabaseWaitingLocksAnalyzer(DatabaseMonitoringService databaseMonitoringService,
                                        ApplicationConfiguration applicationConfiguration) {
        this.databaseMonitoringService = databaseMonitoringService;
        this.applicationConfiguration = applicationConfiguration;
        scheduleRegularLocksRefresh();
    }

    protected void scheduleRegularLocksRefresh() {
        executor.scheduleAtFixedRate(this::refreshLockInfo, 0, POLLING_LOCKS_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
    }

    protected synchronized void refreshLockInfo() {
        deleteObsoleteSamples();
        takeLocksSample();
    }

    private void deleteObsoleteSamples() {
        waitingLocksSamples.removeIf(CachedObject::isExpired);
    }

    private void takeLocksSample() {
        waitingLocksSamples.add(new CachedObject<>(databaseMonitoringService.getProcessesWaitingForLocks(
            ApplicationInstanceNameUtil.buildApplicationInstanceTemplate(applicationConfiguration)),
                                                   MAXIMUM_VALIDITY_OF_LOCKS_SAMPLE_IN_MINUTES));
    }

    public synchronized boolean hasIncreasedDbLocks() {
        int minimumRequiredSamplesCount = (int) (ANOMALY_DETECTION_THRESHOLD_IN_MINUTES.toSeconds() / POLLING_LOCKS_INTERVAL_IN_SECONDS);
        if (waitingLocksSamples.size() < minimumRequiredSamplesCount) {
            LOGGER.debug(MessageFormat.format(Messages.NOT_ENOUGH_SAMPLES_TO_DETECT_ANOMALY_0_1, waitingLocksSamples.size(),
                                              minimumRequiredSamplesCount));
            return false;
        }
        double calculatedIncreasingOrEqualIndex = calculateIncreasingOrEqualIndex();
        boolean hasIncreasedLocks = calculatedIncreasingOrEqualIndex >= MAXIMAL_ACCEPTABLE_INCREMENTAL_LOCKS_DEVIATION_INDEX
            && checkIfLastOneThirdOfSequenceHasIncreasedOrIsEqualComparedToFirstOneThird(minimumRequiredSamplesCount);
        if (shouldLogValues()) {
            LOGGER.info(MessageFormat.format(Messages.VALUES_IN_INSTANCE_IN_THE_WAITING_FOR_LOCKS_SAMPLES,
                                             applicationConfiguration.getApplicationInstanceIndex(), waitingLocksSamples.stream()
                                                                                                                        .map(
                                                                                                                            CachedObject::get)
                                                                                                                        .toList()));
        }
        return hasIncreasedLocks;
    }

    private double calculateIncreasingOrEqualIndex() {
        int increasingOrEqualCount = 0;
        int decreasingCount = 0;
        int totalComparisons = waitingLocksSamples.size() - 1;
        if (waitingLocksSamples.get(0)
                               .get() < MAXIMUM_VALUE_OF_NORMAL_LOCKS_COUNT) {
            return 0;
        }
        for (int i = 1; i < waitingLocksSamples.size(); i++) {
            long currentLocksCount = waitingLocksSamples.get(i)
                                                        .get();
            long previousLocksCount = waitingLocksSamples.get(i - 1)
                                                         .get();
            if (currentLocksCount < MAXIMUM_VALUE_OF_NORMAL_LOCKS_COUNT) {
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
        LOGGER.debug(MessageFormat.format(Messages.INCREASING_OR_EQUAL_INDEX_0_1_2, increasingOrEqualCount, totalComparisons,
                                          increasingOrEqualIndex));
        LOGGER.debug(MessageFormat.format(Messages.DECREASING_INDEX_0_1_2, decreasingCount, totalComparisons, decreasingIndex));
        return increasingOrEqualIndex;
    }

    private boolean checkIfLastOneThirdOfSequenceHasIncreasedOrIsEqualComparedToFirstOneThird(int minimumRequiredSamplesCount) {
        int firstOneThird = minimumRequiredSamplesCount / 3;
        int lastOneThird = waitingLocksSamples.size() - firstOneThird;
        long sumOfFirstOneThird = waitingLocksSamples.subList(0, firstOneThird)
                                                     .stream()
                                                     .mapToLong(CachedObject::get)
                                                     .sum();
        long sumOfLastOneThird = waitingLocksSamples.subList(lastOneThird, waitingLocksSamples.size())
                                                    .stream()
                                                    .mapToLong(CachedObject::get)
                                                    .sum();
        return sumOfLastOneThird >= sumOfFirstOneThird;
    }

    private boolean shouldLogValues() {
        return waitingLocksSamples.stream()
                                  .map(CachedObject::get)
                                  .filter(value -> value >= MAXIMUM_VALUE_OF_NORMAL_LOCKS_COUNT)
                                  .count() >= MINIMUM_REQUIRED_INCREASED_SAMPLES_REQUIRED_FOR_LOGGING;
    }
}
