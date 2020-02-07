package com.sap.cloud.lm.sl.cf.process.listeners;

import javax.inject.Named;

import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;

@Named("deployAppSubProcessEndListener")
public class DeployAppSubProcessEndListener implements ExecutionListener {

    private static final long serialVersionUID = 1L;

    @Override
    public void notify(DelegateExecution context) {
        CloudServiceBroker cloudServiceBrokerExtended = StepsUtil.getCreatedOrUpdatedServiceBroker(context);

        if (cloudServiceBrokerExtended != null) {
            StepsUtil.setVariableInParentProcess(context, Constants.VAR_APP_SERVICE_BROKER_VAR_PREFIX, cloudServiceBrokerExtended);
        }
    }

}
