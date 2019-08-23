package com.sap.cloud.lm.sl.cf.core.health;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OperationFilter;
import com.sap.cloud.lm.sl.cf.core.health.model.Health;
import com.sap.cloud.lm.sl.cf.core.health.model.HealthCheckConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

@Named
public class HealthRetriever {

    private OperationDao operationDao;
    private ApplicationConfiguration configuration;
    private Supplier<ZonedDateTime> currentTimeSupplier;

    @Inject
    public HealthRetriever(OperationDao operationDao, ApplicationConfiguration configuration) {
        this(operationDao, configuration, ZonedDateTime::now);
    }

    protected HealthRetriever(OperationDao operationDao, ApplicationConfiguration configuration,
                              Supplier<ZonedDateTime> currentTimeSupplier) {
        this.operationDao = operationDao;
        this.configuration = configuration;
        this.currentTimeSupplier = currentTimeSupplier;
    }

    public Health getHealth() {
        HealthCheckConfiguration healthCheckConfiguration = configuration.getHealthCheckConfiguration();
        ZonedDateTime currentTime = currentTimeSupplier.get();
        ZonedDateTime xSecondsAgo = currentTime.minusSeconds(healthCheckConfiguration.getTimeRangeInSeconds());
        OperationFilter filter = new OperationFilter.Builder().mtaId(healthCheckConfiguration.getMtaId())
                                                              .spaceId(healthCheckConfiguration.getSpaceId())
                                                              .user(healthCheckConfiguration.getUserName())
                                                              .endedAfter(Date.from(xSecondsAgo.toInstant()))
                                                              .inFinalState()
                                                              .orderByEndTime()
                                                              .descending()
                                                              .build();
        List<Operation> healthCheckOperations = operationDao.find(filter);
        return Health.fromOperations(healthCheckOperations);
    }

}
