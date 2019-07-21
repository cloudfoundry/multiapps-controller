package com.sap.cloud.lm.sl.cf.core.helpers;

import javax.inject.Named;

import com.sap.cloud.lm.sl.mta.model.Module;

@Named
public class ModuleToDeployHelper {

    public boolean isApplication(Module module) {
        return true;
    }

    public boolean shouldDeployAlways(Module module) {
        return false;
    }

}
