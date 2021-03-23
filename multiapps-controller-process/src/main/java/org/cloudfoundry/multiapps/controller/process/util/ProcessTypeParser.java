package org.cloudfoundry.multiapps.controller.process.util;

import java.text.MessageFormat;

import javax.inject.Named;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ProcessTypeParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessTypeParser.class);

    public ProcessType getProcessTypeFromProcessVariable(DelegateExecution execution) {
        String serviceId = VariableHandling.get(execution, Variables.SERVICE_ID);
        if (serviceId == null) {
            String correlationId = VariableHandling.get(execution, Variables.CORRELATION_ID);
            throw new SLException(MessageFormat.format(Messages.UNKNWON_SERVICE_ID_FOR_PROCESS_0, correlationId));
        }
        return getProcessType(serviceId);
    }

    public ProcessType getProcessTypeFromProcessVariable(DelegateExecution execution, boolean required) {
        try {
            return getProcessTypeFromProcessVariable(execution);
        } catch (SLException e) {
            if (required) {
                throw e;
            }
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    public ProcessType getProcessTypeFromExecution(DelegateExecution execution) {
        String processDefinitionId = execution.getProcessDefinitionId();
        int serviceIdEndIndex = processDefinitionId.indexOf(':');

        if (serviceIdEndIndex == -1) {
            String correlationId = VariableHandling.get(execution, Variables.CORRELATION_ID);
            throw new SLException(MessageFormat.format(Messages.UNKNWON_SERVICE_ID_FOR_PROCESS_0, correlationId));
        }

        String serviceId = processDefinitionId.substring(0, serviceIdEndIndex);
        return getProcessType(serviceId);
    }

    protected ProcessType getProcessType(String serviceId) {
        switch (serviceId) {
            case Constants.UNDEPLOY_SERVICE_ID:
                return ProcessType.UNDEPLOY;
            case Constants.DEPLOY_SERVICE_ID:
                return ProcessType.DEPLOY;
            case Constants.BLUE_GREEN_DEPLOY_SERVICE_ID:
                return ProcessType.BLUE_GREEN_DEPLOY;
            default:
                throw new SLException(Messages.UNKNOWN_SERVICE_ID, serviceId);
        }
    }

}
