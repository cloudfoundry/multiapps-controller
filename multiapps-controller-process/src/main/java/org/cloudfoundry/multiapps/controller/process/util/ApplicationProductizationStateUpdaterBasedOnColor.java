package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.controller.core.model.ApplicationColor;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.util.CloudModelBuilderUtil;

public class ApplicationProductizationStateUpdaterBasedOnColor extends ApplicationProductizationStateUpdater {

    private ApplicationColor liveMtaColor;

    public ApplicationProductizationStateUpdaterBasedOnColor(StepLogger stepLogger, ApplicationColor liveMtaColor) {
        super(stepLogger);
        this.liveMtaColor = liveMtaColor;
    }

    @Override
    protected boolean hasIdleLabel(DeployedMtaApplication deployedMtaApplication) {
        return CloudModelBuilderUtil.getApplicationColor(deployedMtaApplication) != liveMtaColor;
    }

}
