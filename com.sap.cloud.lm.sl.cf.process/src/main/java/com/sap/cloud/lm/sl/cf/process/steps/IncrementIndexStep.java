package com.sap.cloud.lm.sl.cf.process.steps;

import javax.inject.Named;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.Messages;

@Named("incrementIndexStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class IncrementIndexStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        // Continue the iteration over the collection:
        String indexVariableName = (String) context.getExecution()
                                                   .getVariable(Constants.VAR_INDEX_VARIABLE_NAME);
        StepsUtil.incrementVariable(context.getExecution(), indexVariableName);

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_INCREMENT_INDEX;
    }

}
