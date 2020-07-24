package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.controller.core.model.BlueGreenApplicationNameSuffix;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;

public class ApplicationProductizationStateUpdaterBasedOnAge extends ApplicationProductizationStateUpdater {

    public ApplicationProductizationStateUpdaterBasedOnAge(StepLogger stepLogger) {
        super(stepLogger);
    }

    @Override
    protected boolean hasIdleLabel(DeployedMtaApplication application) {
        return BlueGreenApplicationNameSuffix.isSuffixContainedIn(application.getName());
    }

}
