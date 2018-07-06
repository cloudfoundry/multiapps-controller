package com.sap.cloud.lm.sl.cf.process.util;

import org.activiti.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.common.SLException;

@Component
public class ProcessTypeParser {

    public ProcessType getProcessType(DelegateExecution context) {
        String serviceId = getServiceId(context);
        switch (serviceId) {
            case Constants.UNDEPLOY_SERVICE_ID:
                return ProcessType.UNDEPLOY;
            case Constants.DEPLOY_SERVICE_ID:
                return ProcessType.DEPLOY;
            case Constants.BLUE_GREEN_DEPLOY_SERVICE_ID:
                return ProcessType.BLUE_GREEN_DEPLOY;
            case Constants.KUBERNETES_DEPLOY_SERVICE_ID:
                return ProcessType.KUBERNETES_DEPLOY;
            case Constants.KUBERNETES_UNDEPLOY_SERVICE_ID:
                return ProcessType.KUBERNETES_UNDEPLOY;
            default:
                throw new SLException(Messages.UNKNOWN_SERVICE_ID, serviceId);
        }
    }

    public static String getServiceId(DelegateExecution context) {
        return (String) context.getVariable(com.sap.cloud.lm.sl.persistence.message.Constants.VARIABLE_NAME_SERVICE_ID);
    }

}
