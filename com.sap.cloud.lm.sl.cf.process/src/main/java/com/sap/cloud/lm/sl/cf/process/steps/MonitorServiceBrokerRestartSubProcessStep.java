package com.sap.cloud.lm.sl.cf.process.steps;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.Constants;

@Component("monitorServiceBrokerRestartSubProcessStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class MonitorServiceBrokerRestartSubProcessStep extends MonitorAppDeploySubProcessStep {

    @Override
    public String getLogicalStepName() {
        return StartServiceBrokerRestartSubProcessStep.class.getSimpleName();
    }

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX;
    }

}
