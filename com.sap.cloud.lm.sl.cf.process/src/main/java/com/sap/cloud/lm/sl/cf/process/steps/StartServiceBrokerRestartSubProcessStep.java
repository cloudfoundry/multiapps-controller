package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Component("startServiceBrokerRestartSubProcessStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StartServiceBrokerRestartSubProcessStep extends StartAppDeploySubProcessStep {

    @Override
    protected Object getIterationVariable(DelegateExecution context, int index) {
        List<CloudApplicationExtended> serviceBrokersToRestart = StepsUtil.getServiceBrokerSubscribersToRestart(context);
        return JsonUtil.toJson(serviceBrokersToRestart.get(index));
    }

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX;
    }
}
