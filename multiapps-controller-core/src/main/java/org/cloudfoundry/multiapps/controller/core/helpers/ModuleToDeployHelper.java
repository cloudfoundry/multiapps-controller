package org.cloudfoundry.multiapps.controller.core.helpers;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.model.Module;

@Named
public class ModuleToDeployHelper {

    public boolean isApplication(Module module) {
        return true;
    }

    public boolean shouldDeployAlways(Module module) {
        return false;
    }

    public boolean shouldSkipDeploy(Module module) {
        Object skipDeployObj = module.getParameters()
                                     .get(SupportedParameters.SKIP_DEPLOY);
        if (skipDeployObj != null) {
            return Boolean.parseBoolean(skipDeployObj.toString());
        }
        return false;
    }

}
