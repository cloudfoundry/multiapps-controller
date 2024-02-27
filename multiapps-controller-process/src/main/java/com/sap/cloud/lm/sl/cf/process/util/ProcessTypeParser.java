package com.sap.cloud.lm.sl.cf.process.util;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.common.SLException;

@Component
@Profile("cf")
public class ProcessTypeParser {

    public static String getServiceId(DelegateExecution context) {
        return (String) context.getVariable(com.sap.cloud.lm.sl.cf.persistence.message.Constants.VARIABLE_NAME_SERVICE_ID);
    }

    public ProcessType getProcessType(DelegateExecution context) {
        String serviceId = getServiceId(context);
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
