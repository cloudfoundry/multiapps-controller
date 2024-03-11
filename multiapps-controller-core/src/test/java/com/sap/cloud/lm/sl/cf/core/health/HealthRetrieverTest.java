package com.sap.cloud.lm.sl.cf.core.health;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OperationFilter;
import com.sap.cloud.lm.sl.cf.core.health.model.Health;
import com.sap.cloud.lm.sl.cf.core.health.model.HealthCheckConfiguration;
import com.sap.cloud.lm.sl.cf.core.health.model.HealthCheckOperation;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.common.util.GenericArgumentMatcher;

public class HealthRetrieverTest {

    private static final String SPACE_ID = "c65d042c-324f-4dc5-a925-9c806acafcfb";
    private static final String MTA_ID = "anatz";
    private static final String USER_NAME = "nictas";
    private static final String OPERATION_ID = "1234";

    private static final long TIME_RANGE_IN_SECONDS = 300;
    private static final long CURRENT_TIME_IN_SECONDS = 500;
    private static final long OPERATION_START_TIME_IN_SECONDS = 0;
    private static final long OPERATION_END_TIME_IN_SECONDS = 250;

    private static final ZonedDateTime CURRENT_TIME = toZonedDateTime(TimeUnit.SECONDS.toMillis(CURRENT_TIME_IN_SECONDS));
    private static final ZonedDateTime OPERATION_START_TIME = toZonedDateTime(TimeUnit.SECONDS.toMillis(OPERATION_START_TIME_IN_SECONDS));
    private static final ZonedDateTime OPERATION_END_TIME = toZonedDateTime(TimeUnit.SECONDS.toMillis(OPERATION_END_TIME_IN_SECONDS));

    @Mock
    private OperationDao operationDao;
    @Mock
    private ApplicationConfiguration configuration;

    private Supplier<ZonedDateTime> currentTimeSupplier = () -> CURRENT_TIME;

    public HealthRetrieverTest() {
        MockitoAnnotations.initMocks(this);
    }

    private static ZonedDateTime toZonedDateTime(long time) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.of("UTC"));
    }

    @Before
    public void prepareConfiguration() {
        HealthCheckConfiguration healthCheckConfiguration = new HealthCheckConfiguration.Builder().mtaId(MTA_ID)
                                                                                                  .spaceId(SPACE_ID)
                                                                                                  .userName(USER_NAME)
                                                                                                  .timeRangeInSeconds(TIME_RANGE_IN_SECONDS)
                                                                                                  .build();
        Mockito.when(configuration.getHealthCheckConfiguration())
               .thenReturn(healthCheckConfiguration);
    }

    @Before
    public void prepareDao() {
        OperationFilter filter = new OperationFilter.Builder().mtaId(MTA_ID)
                                                              .spaceId(SPACE_ID)
                                                              .user(USER_NAME)
                                                              .endedAfter(new Date(TimeUnit.SECONDS.toMillis(CURRENT_TIME_IN_SECONDS
                                                                  - TIME_RANGE_IN_SECONDS)))
                                                              .inFinalState()
                                                              .orderByEndTime()
                                                              .descending()
                                                              .build();
        Operation operation = new Operation().mtaId(MTA_ID)
                                             .spaceId(SPACE_ID)
                                             .user(USER_NAME)
                                             .processId(OPERATION_ID)
                                             .startedAt(OPERATION_START_TIME)
                                             .state(State.FINISHED)
                                             .endedAt(OPERATION_END_TIME);
        Mockito.when(operationDao.find(Mockito.argThat(GenericArgumentMatcher.<OperationFilter> forObject(filter))))
               .thenReturn(Arrays.asList(operation));
    }

    @Test
    public void testGetHealth() {
        HealthRetriever healthRetriever = new HealthRetriever(operationDao, configuration, currentTimeSupplier);

        Health health = healthRetriever.getHealth();

        assertTrue(health.isHealthy());
        assertEquals(1, health.getHealthCheckOperations()
                              .size());

        HealthCheckOperation healthCheckOperation = health.getHealthCheckOperations()
                                                          .get(0);
        assertEquals(OPERATION_ID, healthCheckOperation.getId());
        assertEquals(MTA_ID, healthCheckOperation.getMtaId());
        assertEquals(USER_NAME, healthCheckOperation.getUser());
        assertEquals(SPACE_ID, healthCheckOperation.getSpaceId());
        assertEquals(OPERATION_END_TIME_IN_SECONDS - OPERATION_START_TIME_IN_SECONDS, healthCheckOperation.getDurationInSeconds());
    }

}
