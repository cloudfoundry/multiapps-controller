package org.cloudfoundry.multiapps.controller.core.health.model;

import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.api.model.Operation;

public class Health {

    private final boolean healthy;
    private final List<HealthCheckOperation> healthCheckOperations;

    protected Health(boolean healthy, List<HealthCheckOperation> healthCheckOperations) {
        this.healthy = healthy;
        this.healthCheckOperations = healthCheckOperations;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public List<HealthCheckOperation> getHealthCheckOperations() {
        return healthCheckOperations;
    }

    public static Health fromOperations(List<Operation> operations) {
        List<HealthCheckOperation> healthCheckOperations = toHealthCheckOperations(operations);
        boolean healthy = containsOnlyFinishedOperations(healthCheckOperations);
        return new Health(healthy, healthCheckOperations);
    }

    private static List<HealthCheckOperation> toHealthCheckOperations(List<Operation> operations) {
        return operations.stream()
                         .map(HealthCheckOperation::fromOperation)
                         .collect(Collectors.toList());
    }

    private static boolean containsOnlyFinishedOperations(List<HealthCheckOperation> operations) {
        return operations.stream()
                         .allMatch(operation -> operation.getState() == Operation.State.FINISHED);
    }

}
