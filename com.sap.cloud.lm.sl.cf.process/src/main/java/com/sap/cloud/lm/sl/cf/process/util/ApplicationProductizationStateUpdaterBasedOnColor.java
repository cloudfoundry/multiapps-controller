package com.sap.cloud.lm.sl.cf.process.util;

import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;

public class ApplicationProductizationStateUpdaterBasedOnColor extends ApplicationProductizationStateUpdater {

    private ApplicationColor liveMtaColor;

    public ApplicationProductizationStateUpdaterBasedOnColor(StepLogger stepLogger, ApplicationColor liveMtaColor) {
        super(stepLogger);
        this.liveMtaColor = liveMtaColor;
    }

    @Override
    protected boolean doesApplicationHasIdleLabel(DeployedMtaApplication application) {
        return CloudModelBuilderUtil.getApplicationColor(application) != liveMtaColor;
    }

}
