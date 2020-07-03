package com.sap.cloud.lm.sl.cf.core.health;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.health.model.Health;
import com.sap.cloud.lm.sl.cf.core.health.model.HealthCheckConfiguration;
import com.sap.cloud.lm.sl.cf.core.health.model.HealthCheckOperation;
import com.sap.cloud.lm.sl.cf.core.health.model.ImmutableHealthCheckConfiguration;
import com.sap.cloud.lm.sl.cf.core.persistence.OrderDirection;
import com.sap.cloud.lm.sl.cf.core.persistence.query.OperationQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableOperation;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

class HealthRetrieverTest {

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
    private final Supplier<ZonedDateTime> currentTimeSupplier = () -> CURRENT_TIME;
    @Mock(answer = Answers.RETURNS_SELF)
    private final OperationQuery operationQuery = Mockito.mock(OperationQuery.class);
    @Mock
    private OperationService operationService;
    @Mock
    private ApplicationConfiguration configuration;
    private HealthRetriever healthRetriever;

    public HealthRetrieverTest() {
        MockitoAnnotations.initMocks(this);
    }

    private static ZonedDateTime toZonedDateTime(long time) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.of("UTC"));
    }

    private void prepareConfiguration(HealthCheckConfiguration healthCheckConfiguration) {
        Mockito.when(configuration.getHealthCheckConfiguration())
               .thenReturn(healthCheckConfiguration);
    }

    @BeforeEach
    void prepareOperationService() {
        when(operationService.createQuery()).thenReturn(operationQuery);
        doReturn(Collections.singletonList(getOperation())).when(operationQuery)
                                                           .list();
        healthRetriever = new HealthRetriever(operationService, configuration, currentTimeSupplier);
    }

    private Operation getOperation() {
        return ImmutableOperation.builder()
                                 .mtaId(MTA_ID)
                                 .spaceId(SPACE_ID)
                                 .user(USER_NAME)
                                 .processId(OPERATION_ID)
                                 .startedAt(OPERATION_START_TIME)
                                 .state(Operation.State.FINISHED)
                                 .endedAt(OPERATION_END_TIME)
                                 .build();
    }

    @Test
    void testGetHealth() {
        prepareConfiguration(ImmutableHealthCheckConfiguration.builder()
                                                              .mtaId(MTA_ID)
                                                              .spaceId(SPACE_ID)
                                                              .userName(USER_NAME)
                                                              .timeRangeInSeconds(TIME_RANGE_IN_SECONDS)
                                                              .build());
        Health health = healthRetriever.getHealth();

        assertTrue(health.isHealthy());
        assertEquals(1, health.getHealthCheckOperations()
                              .size());

        HealthCheckOperation healthCheckOperation = health.getHealthCheckOperations()
                                                          .get(0);
        validateHealthCheckOperationParameters(healthCheckOperation);
        Mockito.verify(operationQuery, times(1))
               .user(USER_NAME);
        validateMandatoryParametersAreSet();
    }

    @Test
    void testGetHealthWithoutUsername() {
        prepareConfiguration(ImmutableHealthCheckConfiguration.builder()
                                                              .mtaId(MTA_ID)
                                                              .spaceId(SPACE_ID)
                                                              .timeRangeInSeconds(TIME_RANGE_IN_SECONDS)
                                                              .build());
        Health health = healthRetriever.getHealth();

        assertTrue(health.isHealthy());
        HealthCheckOperation healthCheckOperation = health.getHealthCheckOperations()
                                                          .get(0);
        validateHealthCheckOperationParameters(healthCheckOperation);
        Mockito.verify(operationQuery, never())
               .user(anyString());
        validateMandatoryParametersAreSet();
    }

    private void validateHealthCheckOperationParameters(HealthCheckOperation healthCheckOperation) {
        assertEquals(OPERATION_ID, healthCheckOperation.getId());
        assertEquals(MTA_ID, healthCheckOperation.getMtaId());
        assertEquals(USER_NAME, healthCheckOperation.getUser());
        assertEquals(SPACE_ID, healthCheckOperation.getSpaceId());
        assertEquals(OPERATION_END_TIME_IN_SECONDS - OPERATION_START_TIME_IN_SECONDS, healthCheckOperation.getDurationInSeconds());
    }

    private void validateMandatoryParametersAreSet() {
        Mockito.verify(operationQuery, times(1))
               .mtaId(MTA_ID);
        Mockito.verify(operationQuery, times(1))
               .spaceId(SPACE_ID);
        Mockito.verify(operationQuery, times(1))
               .endedAfter(any());
        Mockito.verify(operationQuery, times(1))
               .inFinalState();
        Mockito.verify(operationQuery, times(1))
               .orderByEndTime(OrderDirection.DESCENDING);
    }

}
