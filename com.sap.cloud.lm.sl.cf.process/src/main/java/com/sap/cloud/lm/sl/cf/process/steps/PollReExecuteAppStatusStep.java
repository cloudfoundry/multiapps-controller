package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("pollReExecuteAppStatusStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PollReExecuteAppStatusStep extends PollExecuteAppStatusStep {

    @Override
    public String getLogicalStepName() {
        return RestartAppStep.class.getSimpleName();
    }

    @Override
    protected CloudApplication getNextApp(DelegateExecution context) {
        return StepsUtil.getAppToRestart(context);
    }

}
