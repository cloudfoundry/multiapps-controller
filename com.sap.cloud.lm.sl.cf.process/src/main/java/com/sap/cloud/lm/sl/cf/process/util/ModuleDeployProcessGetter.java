package com.sap.cloud.lm.sl.cf.process.util;

import javax.inject.Named;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.process.Constants;

@Named
public class ModuleDeployProcessGetter {

    public String get(byte[] moduleToDeploy, DelegateExecution context) {
        return Constants.DEPLOY_APP_SUB_PROCESS_ID;
    }

}
