package com.sap.cloud.lm.sl.cf.process.util;

import com.sap.cloud.lm.sl.cf.core.model.BlueGreenApplicationNameSuffix;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;

public class ApplicationProductizationStateUpdaterBasedOnAge extends ApplicationProductizationStateUpdater {

    public ApplicationProductizationStateUpdaterBasedOnAge(StepLogger stepLogger) {
        super(stepLogger);
    }

    @Override
    protected boolean hasIdleLabel(DeployedMtaApplication application) {
        return BlueGreenApplicationNameSuffix.isSuffixContainedIn(application.getName());
    }

}
