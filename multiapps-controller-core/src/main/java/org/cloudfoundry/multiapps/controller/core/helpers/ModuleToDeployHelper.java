package org.cloudfoundry.multiapps.controller.core.helpers;

import jakarta.inject.Named;

import org.cloudfoundry.multiapps.mta.model.Module;

@Named
public class ModuleToDeployHelper {

    public boolean isApplication(Module module) {
        return true;
    }

    public boolean shouldDeployAlways(Module module) {
        return false;
    }

}
