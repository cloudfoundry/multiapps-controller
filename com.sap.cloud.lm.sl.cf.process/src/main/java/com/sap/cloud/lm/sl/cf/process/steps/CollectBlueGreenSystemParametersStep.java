package com.sap.cloud.lm.sl.cf.process.steps;

import javax.inject.Named;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("collectBlueGreenSystemParametersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CollectBlueGreenSystemParametersStep extends CollectSystemParametersStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        return executeStepInternal(execution, true);
    }

}
