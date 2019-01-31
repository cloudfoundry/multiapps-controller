package com.sap.cloud.lm.sl.cf.process.listeners;

import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceUrl;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;

@Component("deployAppSubProcessEndListener")
public class DeployAppSubProcessEndListener extends AbstractProcessExecutionListener {
    
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(DeployAppSubProcessEndListener.class);

    @Override
    protected void notifyInternal(DelegateExecution context) throws Exception {
        ServiceUrl serviceUrl = StepsUtil.getServiceUrlToRegister(context);
        CloudServiceBroker cloudServiceBrokerExtended = StepsUtil.getCreatedOrUpdatedServiceBroker(context);
        
        if (serviceUrl != null) {
            StepsUtil.setVariableInParentProcess(context, Constants.VAR_APP_SERVICE_URL_VAR_PREFIX, serviceUrl);
        }
        
        if (cloudServiceBrokerExtended != null) {
            StepsUtil.setVariableInParentProcess(context, Constants.VAR_APP_SERVICE_BROKER_VAR_PREFIX, cloudServiceBrokerExtended);
        }
        
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
