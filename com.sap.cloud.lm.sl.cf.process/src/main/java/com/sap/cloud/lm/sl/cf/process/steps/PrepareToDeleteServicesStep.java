package com.sap.cloud.lm.sl.cf.process.steps;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.Constants;

@Component("prepareToDeleteServicesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareToDeleteServicesStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws Exception {
        execution.getContext()
            .setVariable(Constants.VAR_SERVICES_TO_DELETE_LOOP_COUNT, 0);
        return StepPhase.DONE;
    }

}
