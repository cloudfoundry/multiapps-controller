package com.sap.cloud.lm.sl.cf.core.health.model;

import java.util.List;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.State;

public class Health {

    private boolean healthy;
    private List<HealthCheckOperation> healthCheckOperations;

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
        return operations.stream().map(operation -> HealthCheckOperation.fromOperation(operation)).collect(Collectors.toList());
    }

    private static boolean containsOnlyFinishedOperations(List<HealthCheckOperation> operations) {
        return operations.stream().allMatch(operation -> operation.getState() == State.FINISHED);
    }

}
