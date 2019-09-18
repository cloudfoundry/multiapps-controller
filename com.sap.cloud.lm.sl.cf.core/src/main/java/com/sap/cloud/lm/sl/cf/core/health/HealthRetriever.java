package com.sap.cloud.lm.sl.cf.core.health;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import com.sap.cloud.lm.sl.cf.core.health.model.Health;
import com.sap.cloud.lm.sl.cf.core.health.model.HealthCheckConfiguration;
import com.sap.cloud.lm.sl.cf.core.persistence.OrderDirection;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

@Named
public class HealthRetriever {

    private OperationService operationService;
    private ApplicationConfiguration configuration;
    private Supplier<ZonedDateTime> currentTimeSupplier;

    @Inject
    public HealthRetriever(OperationService operationService, ApplicationConfiguration configuration) {
        this(operationService, configuration, ZonedDateTime::now);
    }

    protected HealthRetriever(OperationService operationService, ApplicationConfiguration configuration,
                              Supplier<ZonedDateTime> currentTimeSupplier) {
        this.operationService = operationService;
        this.configuration = configuration;
        this.currentTimeSupplier = currentTimeSupplier;
    }

    public Health getHealth() {
        HealthCheckConfiguration healthCheckConfiguration = configuration.getHealthCheckConfiguration();
        ZonedDateTime currentTime = currentTimeSupplier.get();
        ZonedDateTime xSecondsAgo = currentTime.minusSeconds(healthCheckConfiguration.getTimeRangeInSeconds());
        List<Operation> healthCheckOperations = operationService.createQuery()
                                                                .mtaId(healthCheckConfiguration.getMtaId())
                                                                .spaceId(healthCheckConfiguration.getSpaceId())
                                                                .user(healthCheckConfiguration.getUserName())
                                                                .endedAfter(Date.from(xSecondsAgo.toInstant()))
                                                                .inFinalState()
                                                                .orderByEndTime(OrderDirection.DESCENDING)
                                                                .list();
        return Health.fromOperations(healthCheckOperations);
    }

}
