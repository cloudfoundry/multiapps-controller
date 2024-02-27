package com.sap.cloud.lm.sl.cf.process.steps;

import com.sap.cloud.lm.sl.cf.process.Constants;

public class ModuleDeployProcessGetter {

    public String get(String moduleToDeploy) {
        return Constants.DEPLOY_APP_SUB_PROCESS_ID;
    }

}
