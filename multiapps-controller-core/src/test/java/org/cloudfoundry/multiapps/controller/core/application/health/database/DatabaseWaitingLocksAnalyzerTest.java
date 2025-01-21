package org.cloudfoundry.multiapps.controller.core.application.health.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.DatabaseMonitoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class DatabaseWaitingLocksAnalyzerTest {

    @Mock
    private DatabaseMonitoringService databaseMonitoringService;
    @Mock
    private ApplicationConfiguration applicationConfiguration;

    private DatabaseWaitingLocksAnalyzer databaseWaitingLocksAnalyzer;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        databaseWaitingLocksAnalyzer = new DatabaseWaitingLocksAnalyzerMock(databaseMonitoringService, applicationConfiguration);
    }

    // @formatter:off
    static Stream<Arguments> testIncreasedLocks() {
        return Stream.of(Arguments.of(new long[] { 1, 2, 3, 4, 5, 6, 6, 6, 7, 7, 6, 5, 4, 3, 2, 1, 1, 2, 3, 4, 5, 6, 7, 8, 10, 5, 14, 16, 4, 3, 2, 1 }, false), // has values under the minimal threshold
                         Arguments.of(new long[] { 6, 7, 12, 12, 12, 12, 12, 12, 60, 59, 58, 57, 56, 55, 54, 53, 52, 51, 50, 49, 48, 47, 46, 45, 44, 43, 42, 41, 40, 41, 35, 30 }, false), // most of the values are decreasing, assuming that they will continue to decrease
                         Arguments.of(new long[] { 24, 24, 26, 29, 30, 30, 30, 31, 30, 14, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 11, 12, 13, 14, 14, 15, 16, 17, 18, 20 }, false), // the sum of the last on third of the sequence is smaller compared to the sum of the first one third
                         Arguments.of(new long[] { 24, 24, 26, 29, 30, 30, 30, 31, 30, 14, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 24, 29, 31, 32, 14, 19, 36, 38, 40, 45 }, true), // the index of increase is bigger than the threshold and the sum of the last sequence is bigger than the sum of the first sequence
                         Arguments.of(new long[] { 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7 }, true) // flat sequence
                );
    }
    // @formatter:on

    @ParameterizedTest
    @MethodSource
    void testIncreasedLocks(long[] lockSamples, boolean hasIncreasedLocksExpectation) {
        mockDatabaseWaitingLocksAnalyzer(lockSamples);
        refreshLocksSamples(lockSamples);
        assertEquals(hasIncreasedLocksExpectation, databaseWaitingLocksAnalyzer.hasIncreasedDbLocks());
    }

    private void mockDatabaseWaitingLocksAnalyzer(long[] lockSamples) {
        Mockito.when(databaseMonitoringService.getProcessesWaitingForLocks(anyString()))
               .thenAnswer(invocation -> {
                   int callIndex = Mockito.mockingDetails(databaseMonitoringService)
                                          .getInvocations()
                                          .size()
                       - 1;
                   return lockSamples[callIndex];
               });
    }

    private void refreshLocksSamples(long[] lockSamples) {
        for (int i = 0; i < lockSamples.length; i++) {
            databaseWaitingLocksAnalyzer.refreshLockInfo();
        }
    }

    private static class DatabaseWaitingLocksAnalyzerMock extends DatabaseWaitingLocksAnalyzer {

        public DatabaseWaitingLocksAnalyzerMock(DatabaseMonitoringService databaseMonitoringService,
                                                ApplicationConfiguration applicationConfiguration) {
            super(databaseMonitoringService, applicationConfiguration);
        }

        @Override
        protected void scheduleRegularLocksRefresh() {
            // Do nothing
        }
    }
}
