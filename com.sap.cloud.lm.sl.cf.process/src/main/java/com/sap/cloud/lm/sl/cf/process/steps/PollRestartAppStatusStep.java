package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("pollRestartAppStatusStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PollRestartAppStatusStep extends PollStartAppStatusStep {

    @Override
    public String getLogicalStepName() {
        return RestartAppStep.class.getSimpleName();
    }

    @Override
    protected CloudApplication getAppToPoll(DelegateExecution context) {
        return StepsUtil.getAppToRestart(context);
    }

}
