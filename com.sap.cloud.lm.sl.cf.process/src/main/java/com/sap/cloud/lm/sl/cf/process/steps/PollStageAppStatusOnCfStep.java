package com.sap.cloud.lm.sl.cf.process.steps;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("pollStageAppStatusOnCfStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PollStageAppStatusOnCfStep extends PollStageAppStatusStep {

    @Override
    public String getLogicalStepName() {
        // Staging is not a standalone operation on CF, but is instead part of the start.
        return StartAppStep.class.getSimpleName();
    }

}
