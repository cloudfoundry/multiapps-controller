package org.cloudfoundry.multiapps.controller.core.health;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.health.model.Health;
import org.cloudfoundry.multiapps.controller.core.health.model.HealthCheckConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.OrderDirection;
import org.cloudfoundry.multiapps.controller.persistence.query.OperationQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;

@Named
public class HealthRetriever {

    private final OperationService operationService;
    private final ApplicationConfiguration configuration;
    private final Supplier<ZonedDateTime> currentTimeSupplier;

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
        List<Operation> healthCheckOperations = getHealthCheckOperations(healthCheckConfiguration, xSecondsAgo);
        return Health.fromOperations(healthCheckOperations);
    }

    private List<Operation> getHealthCheckOperations(HealthCheckConfiguration healthCheckConfiguration, ZonedDateTime xSecondsAgo) {
        OperationQuery healthCheckOperationsQuery = operationService.createQuery()
                                                                    .mtaId(healthCheckConfiguration.getMtaId())
                                                                    .spaceId(healthCheckConfiguration.getSpaceId())
                                                                    .endedAfter(Date.from(xSecondsAgo.toInstant()))
                                                                    .inFinalState()
                                                                    .orderByEndTime(OrderDirection.DESCENDING);
        if (healthCheckConfiguration.getUserName() != null) {
            healthCheckOperationsQuery.user(healthCheckConfiguration.getUserName());
        }
        return healthCheckOperationsQuery.list();
    }

}
