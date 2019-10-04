package com.sap.cloud.lm.sl.cf.process.listeners;

import javax.inject.Named;

import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;

@Named("deployAppSubProcessEndListener")
public class DeployAppSubProcessEndListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(DeployAppSubProcessEndListener.class);

    @Override
    protected void notifyInternal(DelegateExecution context) {
        CloudServiceBroker cloudServiceBrokerExtended = StepsUtil.getCreatedOrUpdatedServiceBroker(context);

        if (cloudServiceBrokerExtended != null) {
            StepsUtil.setVariableInParentProcess(context, Constants.VAR_APP_SERVICE_BROKER_VAR_PREFIX, cloudServiceBrokerExtended);
        }
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
