package org.cloudfoundry.multiapps.controller.core.application.health;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.util.ResilientOperationExecutor;
import org.cloudfoundry.multiapps.controller.core.application.health.database.DatabaseWaitingLocksAnalyzer;
import org.cloudfoundry.multiapps.controller.core.application.health.model.ApplicationHealthResult;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.DatabaseHealthService;
import org.cloudfoundry.multiapps.controller.persistence.services.DatabaseMonitoringService;
import org.cloudfoundry.multiapps.controller.persistence.services.JCloudsObjectStoreFileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationHealthCalculatorTest {

    @Mock
    private JCloudsObjectStoreFileStorage jCloudsObjectStoreFileStorage;
    @Mock
    private ApplicationConfiguration applicationConfiguration;
    @Mock
    private DatabaseHealthService databaseHealthService;
    @Mock
    private DatabaseMonitoringService databaseMonitoringService;
    @Mock
    private DatabaseWaitingLocksAnalyzer databaseWaitingLocksAnalyzer;

    private ApplicationHealthCalculator applicationHealthCalculator;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        Mockito.when(applicationConfiguration.isHealthCheckEnabled())
               .thenReturn(true);
        applicationHealthCalculator = new ApplicationHealthCalculatorMock(jCloudsObjectStoreFileStorage,
                                                                          applicationConfiguration,
                                                                          databaseHealthService,
                                                                          databaseMonitoringService,
                                                                          databaseWaitingLocksAnalyzer);
    }

    @Test
    void testUpdateWithFailingObjectStore() {
        Mockito.doThrow(new SLException("Object store not working"))
               .when(jCloudsObjectStoreFileStorage)
               .testConnection();
        applicationHealthCalculator.updateHealthStatus();
        ResponseEntity<ApplicationHealthResult> applicationHealthResultResponseEntity = applicationHealthCalculator.calculateApplicationHealth();
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, applicationHealthResultResponseEntity.getStatusCode());
        assertEquals(ApplicationHealthResult.Status.DOWN, applicationHealthResultResponseEntity.getBody()
                                                                                               .getStatus());
        assertFalse(applicationHealthResultResponseEntity.getBody()
                                                         .hasIncreasedLocks());
    }

    @Test
    void testUpdateWithFailingDatabase() {
        Mockito.doThrow(new SLException("Database not working"))
               .when(databaseHealthService)
               .testDatabaseConnection();
        applicationHealthCalculator.updateHealthStatus();
        ResponseEntity<ApplicationHealthResult> applicationHealthResultResponseEntity = applicationHealthCalculator.calculateApplicationHealth();
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, applicationHealthResultResponseEntity.getStatusCode());
        assertEquals(ApplicationHealthResult.Status.DOWN, applicationHealthResultResponseEntity.getBody()
                                                                                               .getStatus());
        assertFalse(applicationHealthResultResponseEntity.getBody()
                                                         .hasIncreasedLocks());
    }

    @Test
    void testSuccessfulHealthCheck() {
        applicationHealthCalculator.updateHealthStatus();
        ResponseEntity<ApplicationHealthResult> applicationHealthResultResponseEntity = applicationHealthCalculator.calculateApplicationHealth();
        assertEquals(HttpStatus.OK, applicationHealthResultResponseEntity.getStatusCode());
        assertEquals(ApplicationHealthResult.Status.UP, applicationHealthResultResponseEntity.getBody()
                                                                                             .getStatus());
        assertFalse(applicationHealthResultResponseEntity.getBody()
                                                         .hasIncreasedLocks());
    }

    @Test
    void testUpdateWithIncreasedDatabaseLocks() {
        Mockito.when(databaseWaitingLocksAnalyzer.hasIncreasedDbLocks())
               .thenReturn(true);
        applicationHealthCalculator.updateHealthStatus();
        ResponseEntity<ApplicationHealthResult> applicationHealthResultResponseEntity = applicationHealthCalculator.calculateApplicationHealth();
        assertEquals(HttpStatus.OK, applicationHealthResultResponseEntity.getStatusCode());
        assertEquals(ApplicationHealthResult.Status.DOWN, applicationHealthResultResponseEntity.getBody()
                                                                                               .getStatus());
        assertTrue(applicationHealthResultResponseEntity.getBody()
                                                        .hasIncreasedLocks());
    }

    @Test
    void testSuccessfulUpdateWithMissingObjectStore() {
        applicationHealthCalculator = new ApplicationHealthCalculatorMock(null,
                                                                          applicationConfiguration,
                                                                          databaseHealthService,
                                                                          databaseMonitoringService,
                                                                          databaseWaitingLocksAnalyzer);
        applicationHealthCalculator.updateHealthStatus();
        ResponseEntity<ApplicationHealthResult> applicationHealthResultResponseEntity = applicationHealthCalculator.calculateApplicationHealth();
        assertEquals(HttpStatus.OK, applicationHealthResultResponseEntity.getStatusCode());
        assertEquals(ApplicationHealthResult.Status.UP, applicationHealthResultResponseEntity.getBody()
                                                                                             .getStatus());
        assertFalse(applicationHealthResultResponseEntity.getBody()
                                                         .hasIncreasedLocks());
    }

    private static class ApplicationHealthCalculatorMock extends ApplicationHealthCalculator {
        public ApplicationHealthCalculatorMock(JCloudsObjectStoreFileStorage JCloudsObjectStoreFileStorage,
                                               ApplicationConfiguration applicationConfiguration,
                                               DatabaseHealthService databaseHealthService,
                                               DatabaseMonitoringService databaseMonitoringService,
                                               DatabaseWaitingLocksAnalyzer databaseWaitingLocksAnalyzer) {
            super(JCloudsObjectStoreFileStorage,
                  applicationConfiguration,
                  databaseHealthService,
                  databaseMonitoringService,
                  databaseWaitingLocksAnalyzer);
        }

        @Override
        protected ResilientOperationExecutor getResilienceExecutor() {
            return new ResilientOperationExecutor().withRetryCount(0)
                                                   .withWaitTimeBetweenRetriesInMillis(0);
        }

        @Override
        protected void scheduleRegularHealthUpdate() {
            // Do nothing
        }
    }
}
