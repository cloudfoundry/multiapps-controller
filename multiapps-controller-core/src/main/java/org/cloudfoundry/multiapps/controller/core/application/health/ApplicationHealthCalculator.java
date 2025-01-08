package org.cloudfoundry.multiapps.controller.core.application.health;

import java.time.Duration;
import java.util.function.Supplier;

import org.cloudfoundry.multiapps.controller.client.util.ResilientOperationExecutor;
import org.cloudfoundry.multiapps.controller.core.application.health.database.DatabaseWaitingLocksAnalyzer;
import org.cloudfoundry.multiapps.controller.core.application.health.model.ApplicationHealthResult;
import org.cloudfoundry.multiapps.controller.core.application.health.model.ImmutableApplicationHealthResult;
import org.cloudfoundry.multiapps.controller.core.model.CachedObject;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationInstanceNameUtil;
import org.cloudfoundry.multiapps.controller.persistence.services.DatabaseHealthService;
import org.cloudfoundry.multiapps.controller.persistence.services.DatabaseMonitoringService;
import org.cloudfoundry.multiapps.controller.persistence.services.ObjectStoreFileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.inject.Named;

@Named
public class ApplicationHealthCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationHealthCalculator.class.getName());

    private final ObjectStoreFileStorage objectStoreFileStorage;
    private final ApplicationConfiguration applicationConfiguration;
    private final DatabaseHealthService databaseHealthService;
    private final DatabaseMonitoringService databaseMonitoringService;
    private final DatabaseWaitingLocksAnalyzer databaseWaitingLocksAnalyzer;

    private final CachedObject<Boolean> objectStoreFileStorageHealthCache = new CachedObject<>(Duration.ofSeconds(15));
    private final CachedObject<Boolean> dbHealthServiceCache = new CachedObject<>(Duration.ofSeconds(15));

    @Autowired
    public ApplicationHealthCalculator(@Autowired(required = false) ObjectStoreFileStorage objectStoreFileStorage,
                                       ApplicationConfiguration applicationConfiguration, DatabaseHealthService databaseHealthService,
                                       DatabaseMonitoringService databaseMonitoringService,
                                       DatabaseWaitingLocksAnalyzer databaseWaitingLocksAnalyzer) {
        this.objectStoreFileStorage = objectStoreFileStorage;
        this.applicationConfiguration = applicationConfiguration;
        this.databaseHealthService = databaseHealthService;
        this.databaseMonitoringService = databaseMonitoringService;
        this.databaseWaitingLocksAnalyzer = databaseWaitingLocksAnalyzer;
    }

    public ResponseEntity<ApplicationHealthResult> calculateApplicationHealth() {
        boolean isObjectStoreFileStorageHealthy = objectStoreFileStorageHealthCache.getOrRefresh(this::isObjectStoreFileStorageHealthy);
        boolean isDbHealthy = dbHealthServiceCache.getOrRefresh(() -> getResilienceExecutor().execute((Supplier<Boolean>) databaseHealthService::isDatabaseHealthy));
        if (!isObjectStoreFileStorageHealthy || !isDbHealthy) {
            LOGGER.error("Object store file storage health: \"{}\", Database health: \"{}\"", isObjectStoreFileStorageHealthy, isDbHealthy);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                 .body(ImmutableApplicationHealthResult.builder()
                                                                       .status(ApplicationHealthResult.Status.DOWN)
                                                                       .hasIncreasedLocks(false)
                                                                       .build());
        }
        boolean hasIncreasedDbLocks = databaseWaitingLocksAnalyzer.hasIncreasedDbLocks();
        if (hasIncreasedDbLocks) {
            long countOfProcessesWaitingForLocks = getResilienceExecutor().execute((Supplier<Long>) () -> databaseMonitoringService.getProcessesWaitingForLocks(ApplicationInstanceNameUtil.buildApplicationInstanceTemplate(applicationConfiguration)));
            LOGGER.warn("Detected increased number of processes waiting for locks: \"{}\" for instance: \"{}\"",
                        countOfProcessesWaitingForLocks, applicationConfiguration.getApplicationInstanceIndex());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE) // make it 503
                                 .body(ImmutableApplicationHealthResult.builder()
                                                                       .status(ApplicationHealthResult.Status.DOWN)
                                                                       .hasIncreasedLocks(true)
                                                                       .countOfProcessesWaitingForLocks(countOfProcessesWaitingForLocks)
                                                                       .build());
        }
        long countOfProcessesWaitingForLocks = getResilienceExecutor().execute((Supplier<Long>) () -> databaseMonitoringService.getProcessesWaitingForLocks(ApplicationInstanceNameUtil.buildApplicationInstanceTemplate(applicationConfiguration)));
        return ResponseEntity.ok(ImmutableApplicationHealthResult.builder()
                                                                 .status(ApplicationHealthResult.Status.UP)
                                                                 .hasIncreasedLocks(false)
                                                                 .countOfProcessesWaitingForLocks(countOfProcessesWaitingForLocks)
                                                                 .build());
    }

    private boolean isObjectStoreFileStorageHealthy() {
        if (objectStoreFileStorage == null) {
            LOGGER.debug("Object store file storage is not available for instance: \"{}\"",
                         applicationConfiguration.getApplicationInstanceIndex());
            return true;
        }
        try {
            getResilienceExecutor().execute(objectStoreFileStorage::testConnection);
        } catch (Exception e) {
            LOGGER.error("Error occurred during object store health checking for instance: \"{}\"",
                         applicationConfiguration.getApplicationInstanceIndex(), e);
            return false;
        }
        return true;
    }

    private ResilientOperationExecutor getResilienceExecutor() {
        return new ResilientOperationExecutor();
    }

}
