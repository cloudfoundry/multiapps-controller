package com.sap.cloud.lm.sl.cf.process.steps;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("collectBlueGreenSystemParametersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CollectBlueGreenSystemParametersStep extends CollectSystemParametersStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        return executeStepInternal(execution, true);
    }

}
