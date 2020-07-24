package org.cloudfoundry.multiapps.controller.process.util;

import javax.inject.Named;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;

@Named
public class ProcessTypeParser {

    public ProcessType getProcessType(DelegateExecution execution) {
        String serviceId = getServiceId(execution);
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

    public static String getServiceId(DelegateExecution execution) {
        return VariableHandling.get(execution, Variables.SERVICE_ID);
    }

}
