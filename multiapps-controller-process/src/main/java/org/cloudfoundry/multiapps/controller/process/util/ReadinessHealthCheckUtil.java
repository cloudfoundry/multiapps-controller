package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public class ReadinessHealthCheckUtil {

    private ReadinessHealthCheckUtil() {
    }

    public static boolean shouldWaitForAppToBecomeRoutable(ProcessContext context) {
        CloudApplicationExtended appToProcess = context.getVariable(Variables.APP_TO_PROCESS);
        return appToProcess.getStaging()
                           .getReadinessHealthCheckType() != null;
    }
}
