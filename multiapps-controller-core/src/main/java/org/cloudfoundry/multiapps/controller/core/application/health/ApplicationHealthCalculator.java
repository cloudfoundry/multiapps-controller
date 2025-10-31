package org.cloudfoundry.multiapps.controller.core.application.health;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.util.CheckedSupplier;
import org.cloudfoundry.multiapps.controller.client.util.ResilientOperationExecutor;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.application.health.database.DatabaseWaitingLocksAnalyzer;
import org.cloudfoundry.multiapps.controller.core.application.health.model.ApplicationHealthResult;
import org.cloudfoundry.multiapps.controller.core.application.health.model.ImmutableApplicationHealthResult;
import org.cloudfoundry.multiapps.controller.core.model.CachedObject;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationInstanceNameUtil;
import org.cloudfoundry.multiapps.controller.persistence.services.DatabaseHealthService;
import org.cloudfoundry.multiapps.controller.persistence.services.DatabaseMonitoringService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Named
public class ApplicationHealthCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationHealthCalculator.class);

    private static final int UPDATE_HEALTH_CHECK_STATUS_PERIOD_IN_SECONDS = 10;
    private static final int SINGLE_TASK_TIMEOUT_IN_SECONDS = 70; // timeout is set to 70 so it is higher than the DB connection acquisition
    // timeout
    private static final int TOTAL_TASK_TIMEOUT_IN_SECONDS = 3 * SINGLE_TASK_TIMEOUT_IN_SECONDS;

    private final FileStorage objectStoreFileStorage;
    private final ApplicationConfiguration applicationConfiguration;
    private final DatabaseHealthService databaseHealthService;
    private final DatabaseMonitoringService databaseMonitoringService;
    private final DatabaseWaitingLocksAnalyzer databaseWaitingLocksAnalyzer;

    private final CachedObject<Boolean> objectStoreFileStorageHealthCache = new CachedObject<>(
        Duration.ofSeconds(TOTAL_TASK_TIMEOUT_IN_SECONDS));
    private final CachedObject<Boolean> dbHealthServiceCache = new CachedObject<>(Duration.ofSeconds(TOTAL_TASK_TIMEOUT_IN_SECONDS));
    private final CachedObject<Boolean> hasIncreasedLocksCache = new CachedObject<>(false,
                                                                                    Duration.ofSeconds(TOTAL_TASK_TIMEOUT_IN_SECONDS));
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService taskExecutor = new ThreadPoolExecutor(3,
                                                                        9,
                                                                        0L,
                                                                        TimeUnit.MILLISECONDS,
                                                                        new SynchronousQueue<>(),
                                                                        new ThreadPoolExecutor.AbortPolicy());
    private final ExecutorService timeoutExecutor = new ThreadPoolExecutor(3,
                                                                           9,
                                                                           0L,
                                                                           TimeUnit.MILLISECONDS,
                                                                           new SynchronousQueue<>(),
                                                                           new ThreadPoolExecutor.AbortPolicy());

    private final ResilientOperationExecutor resilientOperationExecutor = getResilienceExecutor();

    @Inject
    public ApplicationHealthCalculator(@Autowired(required = false) FileStorage objectStoreFileStorage,
                                       ApplicationConfiguration applicationConfiguration, DatabaseHealthService databaseHealthService,
                                       DatabaseMonitoringService databaseMonitoringService,
                                       DatabaseWaitingLocksAnalyzer databaseWaitingLocksAnalyzer) {
        this.objectStoreFileStorage = objectStoreFileStorage;
        this.applicationConfiguration = applicationConfiguration;
        this.databaseHealthService = databaseHealthService;
        this.databaseMonitoringService = databaseMonitoringService;
        this.databaseWaitingLocksAnalyzer = databaseWaitingLocksAnalyzer;
        scheduleRegularHealthUpdate();
    }

    protected void scheduleRegularHealthUpdate() {
        scheduler.scheduleAtFixedRate(this::updateHealthStatus, 0, UPDATE_HEALTH_CHECK_STATUS_PERIOD_IN_SECONDS, TimeUnit.SECONDS);
    }

    protected void updateHealthStatus() {
        List<Callable<Boolean>> tasks = List.of(this::isObjectStoreFileStorageHealthy, this::isDatabaseHealthy,
                                                this::checkForIncreasedLocksWithTimeout);
        try {
            List<Future<Boolean>> completedFutures = taskExecutor.invokeAll(tasks, TOTAL_TASK_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
            executeFuture(completedFutures.get(0), isHealthy -> objectStoreFileStorageHealthCache.refresh(() -> isHealthy), false,
                          Messages.ERROR_OCCURRED_DURING_OBJECT_STORE_HEALTH_CHECKING);
            executeFuture(completedFutures.get(1), isHealthy -> dbHealthServiceCache.refresh(() -> isHealthy), false,
                          Messages.ERROR_OCCURRED_DURING_DATABASE_HEALTH_CHECKING);
            executeFuture(completedFutures.get(2), hasIncreasedLocks -> hasIncreasedLocksCache.refresh(() -> hasIncreasedLocks), true,
                          Messages.ERROR_OCCURRED_WHILE_CHECKING_FOR_INCREASED_LOCKS);
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
            LOGGER.error(Messages.THREAD_WAS_INTERRUPTED_WHILE_WAITING_FOR_THE_RESULT_OF_A_FUTURE, e);
            dbHealthServiceCache.refresh(() -> false);
            objectStoreFileStorageHealthCache.refresh(() -> false);
            hasIncreasedLocksCache.refresh(() -> false);
        }
    }

    private void executeFuture(Future<Boolean> future, Consumer<Boolean> consumer, boolean onErrorValue, String errorMessage) {
        try {
            Boolean result = future.get();
            consumer.accept(result);
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
            LOGGER.error(Messages.THREAD_WAS_INTERRUPTED_WHILE_WAITING_FOR_THE_RESULT_OF_A_FUTURE, e);
            future.cancel(true);
            consumer.accept(onErrorValue);
        } catch (Exception e) {
            LOGGER.error(MessageFormat.format(Messages.ERROR_OCCURRED_DURING_HEALTH_CHECKING_FOR_INSTANCE_0_MESSAGE_1,
                                              applicationConfiguration.getApplicationInstanceIndex(), errorMessage),
                         e);
            future.cancel(true);
            consumer.accept(onErrorValue);
        }
    }

    public ResponseEntity<ApplicationHealthResult> calculateApplicationHealth() {
        if (!applicationConfiguration.isHealthCheckEnabled()) {
            return ResponseEntity.ok(ImmutableApplicationHealthResult.builder()
                                                                     .status(ApplicationHealthResult.Status.UP)
                                                                     .hasIncreasedLocks(false)
                                                                     .build());
        }
        boolean isObjectStoreFileStorageHealthy = objectStoreFileStorageHealthCache.getOrRefresh(() -> false);
        boolean isDbHealthy = dbHealthServiceCache.getOrRefresh(() -> false);

        if (!isObjectStoreFileStorageHealthy || !isDbHealthy) {
            LOGGER.error(MessageFormat.format(Messages.OBJECT_STORE_FILE_STORAGE_HEALTH_DATABASE_HEALTH, isObjectStoreFileStorageHealthy,
                                              isDbHealthy));
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                 .body(ImmutableApplicationHealthResult.builder()
                                                                       .status(ApplicationHealthResult.Status.DOWN)
                                                                       .hasIncreasedLocks(false)
                                                                       .build());
        }
        boolean hasIncreasedDbLocks = hasIncreasedLocksCache.getOrRefresh(() -> true);
        if (hasIncreasedDbLocks) {
            LOGGER.warn(
                MessageFormat.format(Messages.DETECTED_INCREASED_NUMBER_OF_PROCESSES_WAITING_FOR_LOCKS_FOR_INSTANCE_0_GETTING_THE_LOCKS,
                                     applicationConfiguration.getApplicationInstanceIndex()));
            long countOfProcessesWaitingForLocks = resilientOperationExecutor.execute(
                (Supplier<Long>) () -> databaseMonitoringService.getProcessesWaitingForLocks(
                    ApplicationInstanceNameUtil.buildApplicationInstanceTemplate(applicationConfiguration)));
            LOGGER.warn(MessageFormat.format(Messages.DETECTED_INCREASED_NUMBER_OF_PROCESSES_WAITING_FOR_LOCKS_FOR_INSTANCE,
                                             countOfProcessesWaitingForLocks, applicationConfiguration.getApplicationInstanceIndex()));
            return ResponseEntity.ok(ImmutableApplicationHealthResult.builder() // TODO: Make this return 503 instead of 200 when the
                                                                     // detection is trustworthy
                                                                     .status(ApplicationHealthResult.Status.DOWN)
                                                                     .hasIncreasedLocks(true)
                                                                     .countOfProcessesWaitingForLocks(countOfProcessesWaitingForLocks)
                                                                     .build());
        }
        return ResponseEntity.ok(ImmutableApplicationHealthResult.builder()
                                                                 .status(ApplicationHealthResult.Status.UP)
                                                                 .hasIncreasedLocks(false)
                                                                 .build());
    }

    private boolean isObjectStoreFileStorageHealthy() {
        if (objectStoreFileStorage == null) {
            LOGGER.debug(MessageFormat.format(Messages.OBJECT_STORE_FILE_STORAGE_IS_NOT_AVAILABLE_FOR_INSTANCE,
                                              applicationConfiguration.getApplicationInstanceIndex()));
            return true;
        }
        try {
            resilientOperationExecutor.execute((CheckedSupplier<Boolean>) this::testObjectStoreConnectionWithTimeout);
        } catch (Exception e) {
            LOGGER.error(MessageFormat.format(Messages.ERROR_OCCURRED_DURING_OBJECT_STORE_HEALTH_CHECKING_FOR_INSTANCE,
                                              applicationConfiguration.getApplicationInstanceIndex()),
                         e);
            return false;
        }
        return true;
    }

    private boolean testObjectStoreConnectionWithTimeout() throws ExecutionException, InterruptedException {
        Future<Boolean> future = timeoutExecutor.submit(() -> {
            objectStoreFileStorage.testConnection();
            return true;
        });
        try {
            LOGGER.debug(Messages.CHECKING_OBJECT_STORE_HEALTH);
            return future.get(SINGLE_TASK_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new SLException(e, Messages.TIMEOUT_WHILE_CHECKING_OBJECT_STORE_HEALTH);
        }
    }

    private boolean isDatabaseHealthy() {
        try {
            resilientOperationExecutor.execute((CheckedSupplier<Boolean>) this::testDatabaseConnectionWithTimeout);
            return true;
        } catch (Exception e) {
            LOGGER.error(MessageFormat.format(Messages.ERROR_OCCURRED_WHILE_CHECKING_DATABASE_INSTANCE_0,
                                              applicationConfiguration.getApplicationInstanceIndex()),
                         e);
            return false;
        }
    }

    private boolean testDatabaseConnectionWithTimeout() throws ExecutionException, InterruptedException {
        Future<Boolean> future = timeoutExecutor.submit(() -> {
            databaseHealthService.testDatabaseConnection();
            return true;
        });
        try {
            LOGGER.debug(Messages.CHECKING_DATABASE_HEALTH);
            return future.get(SINGLE_TASK_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new SLException(e, Messages.TIMEOUT_WHILE_CHECKING_DATABASE_HEALTH);
        }
    }

    private boolean checkForIncreasedLocksWithTimeout() throws ExecutionException, InterruptedException {
        Future<Boolean> future = timeoutExecutor.submit(databaseWaitingLocksAnalyzer::hasIncreasedDbLocks);
        try {
            LOGGER.debug(Messages.CHECKING_FOR_INCREASED_LOCKS);
            return future.get(SINGLE_TASK_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new SLException(e, Messages.TIMEOUT_WHILE_CHECKING_FOR_INCREASED_LOCKS);
        }
    }

    protected ResilientOperationExecutor getResilienceExecutor() {
        return new ResilientOperationExecutor();
    }

}
