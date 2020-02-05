package com.sap.cloud.lm.sl.cf.process.util;

import java.util.List;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

public abstract class ApplicationProductizationStateUpdater {

    private StepLogger stepLogger;

    public ApplicationProductizationStateUpdater(StepLogger stepLogger) {
        this.stepLogger = stepLogger;
    }

    public List<DeployedMtaApplication> updateApplicationsProductizationState(List<DeployedMtaApplication> applications) {
        return applications.stream()
                           .map(this::updateApplicationProductizationState)
                           .collect(Collectors.toList());
    }

    private DeployedMtaApplication updateApplicationProductizationState(DeployedMtaApplication deployedMtaApplication) {
        if (hasIdleLabel(deployedMtaApplication)) {
            stepLogger.debug(Messages.MODULE_WITH_APPLICATION_NAME_WAS_MARKED_AS_IDLE, deployedMtaApplication.getModuleName(),
                             deployedMtaApplication.getName());
            return ImmutableDeployedMtaApplication.builder()
                                                  .from(deployedMtaApplication)
                                                  .productizationState(DeployedMtaApplication.ProductizationState.IDLE)
                                                  .build();
        }
        return deployedMtaApplication;
    }

    protected abstract boolean hasIdleLabel(DeployedMtaApplication application);

}
