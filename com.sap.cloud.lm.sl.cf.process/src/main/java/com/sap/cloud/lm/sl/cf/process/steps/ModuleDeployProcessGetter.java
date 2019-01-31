package com.sap.cloud.lm.sl.cf.process.steps;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.process.Constants;

public class ModuleDeployProcessGetter {

    public String get(byte[] moduleToDeploy, DelegateExecution context) {
        return Constants.DEPLOY_APP_SUB_PROCESS_ID;
    }

}
