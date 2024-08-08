package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.variables.Variable;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;

import java.time.Duration;

public class TimeoutGlobalParameters extends GlobalParameters {
    private final int startTimeout;
    private final int stageTimeout;
    private final int uploadTimeout;
    private final int taskExecutionTimeout;

    public TimeoutGlobalParameters(DeploymentDescriptor deploymentDescriptor) {
        super(deploymentDescriptor);
        startTimeout = getTimeoutGlobalParameter(deploymentDescriptor, SupportedParameters.APPS_START_TIMEOUT,
                                                 Variables.APPS_START_TIMEOUT_GLOBAL_LEVEL);
        stageTimeout = getTimeoutGlobalParameter(deploymentDescriptor, SupportedParameters.APPS_STAGE_TIMEOUT,
                                                 Variables.APPS_STAGE_TIMEOUT_GLOBAL_LEVEL);
        uploadTimeout = getTimeoutGlobalParameter(deploymentDescriptor, SupportedParameters.APPS_UPLOAD_TIMEOUT,
                                                  Variables.APPS_UPLOAD_TIMEOUT_GLOBAL_LEVEL);
        taskExecutionTimeout = getTimeoutGlobalParameter(deploymentDescriptor, SupportedParameters.TASK_EXECUTION_TIMEOUT,
                                                         Variables.TASK_EXECUTION_TIMEOUT_GLOBAL_LEVEL);
    }

    private int getTimeoutGlobalParameter(DeploymentDescriptor deploymentDescriptor, String timeoutName,
                                          Variable<Duration> timeoutGlobalLevel) {
        return ((Number) deploymentDescriptor.getParameters()
                                             .getOrDefault(timeoutName, timeoutGlobalLevel.getDefaultValue()
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
