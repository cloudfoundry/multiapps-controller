package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Date;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.common.ConflictException;

public class ApplicationColorDetector {

    private static final ApplicationColor COLOR_OF_APPLICATIONS_WITHOUT_SUFFIX = ApplicationColor.BLUE;

    public ApplicationColor detectFirstDeployedApplicationColor(DeployedMta deployedMta) {
        if (deployedMta == null) {
            return null;
        }
        ApplicationColor firstApplicationColor = null;
        Date firstApplicationColorCreatedOn = null;
        for (DeployedMtaModule module : deployedMta.getModules()) {
            Date moduleCreatedOn = module.getCreatedOn();
            if (firstApplicationColorCreatedOn == null || firstApplicationColorCreatedOn.after(moduleCreatedOn)) {
                firstApplicationColorCreatedOn = moduleCreatedOn;
                firstApplicationColor = getApplicationColor(module);
            }
        }
        return firstApplicationColor;
    }

    public ApplicationColor detectSingularDeployedApplicationColor(DeployedMta deployedMta) {
        if (deployedMta == null) {
            return null;
        }
        ApplicationColor deployedApplicationColor = null;
        for (DeployedMtaModule module : deployedMta.getModules()) {
            ApplicationColor moduleApplicationColor = getApplicationColor(module);
            if (deployedApplicationColor == null) {
                deployedApplicationColor = (moduleApplicationColor);
            }
            if (deployedApplicationColor != moduleApplicationColor) {
                throw new ConflictException(Messages.CONFLICTING_APP_COLORS, deployedMta.getMetadata().getId());
            }
        }
        return deployedApplicationColor;
    }

    private ApplicationColor getApplicationColor(DeployedMtaModule deployedMtaModule) {
        for (ApplicationColor color : ApplicationColor.values()) {
            if (deployedMtaModule.getAppName().endsWith(color.asSuffix())) {
                return color;
            }
        }
        return COLOR_OF_APPLICATIONS_WITHOUT_SUFFIX;
    }

}
