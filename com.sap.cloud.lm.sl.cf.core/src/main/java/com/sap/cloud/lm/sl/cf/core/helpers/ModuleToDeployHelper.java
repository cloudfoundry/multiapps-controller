package com.sap.cloud.lm.sl.cf.core.helpers;

import com.sap.cloud.lm.sl.cf.core.model.ModuleToDeploy;
import com.sap.cloud.lm.sl.mta.model.v2.Module;

public class ModuleToDeployHelper {

    public boolean isApplication(ModuleToDeploy module) {
        return true;
    }
    
    public boolean isApplication(Module module) {
        return true;
    }

    public boolean shouldDeployAlways(Module module) {
        return false;
    }

}
