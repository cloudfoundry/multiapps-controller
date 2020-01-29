package com.sap.cloud.lm.sl.cf.process.util;

import java.util.List;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
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

    private DeployedMtaApplication updateApplicationProductizationState(DeployedMtaApplication application) {
        if (doesApplicationHasIdleLabel(application)) {
            application.setProductizationState(DeployedMtaApplication.ProductizationState.IDLE);
            stepLogger.debug(Messages.MODULE_WITH_APPLICATION_NAME_WAS_MARKED_AS_IDLE, application.getModuleName(),
                             application.getAppName());
        }
        return application;
    }

    protected abstract boolean doesApplicationHasIdleLabel(DeployedMtaApplication application);

}
