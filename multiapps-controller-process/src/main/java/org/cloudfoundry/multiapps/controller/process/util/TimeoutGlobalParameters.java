package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;

public class TimeoutGlobalParameters extends GlobalParameters {
    private final int startTimeout;
    private final int stageTimeout;
    private final int uploadTimeout;
    private final int taskExecutionTimeout;

    public TimeoutGlobalParameters(DeploymentDescriptor deploymentDescriptor) {
        super(deploymentDescriptor);
        startTimeout = ((Number) deploymentDescriptor.getParameters()
                                                     .getOrDefault(SupportedParameters.APPS_START_TIMEOUT,
                                                                   Variables.START_APP_TIMEOUT_GLOBAL.getDefaultValue()
                                                                                                     .toSeconds())).intValue();
        stageTimeout = ((Number) deploymentDescriptor.getParameters()
                                                     .getOrDefault(SupportedParameters.APPS_STAGE_TIMEOUT,
                                                                   Variables.STAGE_APP_TIMEOUT_GLOBAL.getDefaultValue()
                                                                                                     .toSeconds())).intValue();
        uploadTimeout = ((Number) deploymentDescriptor.getParameters()
                                                      .getOrDefault(SupportedParameters.APPS_UPLOAD_TIMEOUT,
                                                                    Variables.UPLOAD_APP_TIMEOUT_GLOBAL.getDefaultValue()
                                                                                                       .toSeconds())).intValue();
        taskExecutionTimeout = ((Number) deploymentDescriptor.getParameters()
                                                             .getOrDefault(SupportedParameters.TASK_EXECUTION_TIMEOUT,
                                                                           Variables.TASK_EXECUTION_TIMEOUT_GLOBAL.getDefaultValue()
                                                                                                                  .toSeconds())).intValue();
    }

    public int getStartTimeout() {
        return startTimeout;
    }

    public int getStageTimeout() {
        return stageTimeout;
    }

    public int getUploadTimeout() {
        return uploadTimeout;
    }

    public int getTaskExecutionTimeout() {
        return taskExecutionTimeout;
    }

}
