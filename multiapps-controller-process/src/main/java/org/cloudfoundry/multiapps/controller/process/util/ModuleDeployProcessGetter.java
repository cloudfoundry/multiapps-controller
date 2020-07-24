package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.controller.process.Constants;
import org.flowable.engine.delegate.DelegateExecution;

public class ModuleDeployProcessGetter {

    public String get(byte[] moduleToDeploy, DelegateExecution execution) {
        return Constants.DEPLOY_APP_SUB_PROCESS_ID;
    }

}
