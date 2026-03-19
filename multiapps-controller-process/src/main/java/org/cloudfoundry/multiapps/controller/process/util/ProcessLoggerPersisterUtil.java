package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.ProcessLoggerPersisterConfiguration;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;

public class ProcessLoggerPersisterUtil {

    public static ProcessLoggerPersisterConfiguration createProcessLoggerPersisterConfiguration(
        DelegateExecution delegateExecution) {
        String correlationId = VariableHandling.get(delegateExecution, Variables.CORRELATION_ID);
        String taskId = VariableHandling.get(delegateExecution, Variables.TASK_ID);
        LoggingConfiguration loggingConfiguration = VariableHandling.get(delegateExecution,
                                                                         Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION);
        boolean isExternalLoggingConfigurationEnabled = VariableHandling.get(delegateExecution,
                                                                             Variables.IS_EXTERNAL_LOGGING_SERVICE_ENABLED);

        return new ProcessLoggerPersisterConfiguration(correlationId, taskId, loggingConfiguration,
                                                       isExternalLoggingConfigurationEnabled);
    }
}
