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
    protected StepPhase executeStep(ExecutionWrapper execution) {
        // Continue the iteration over the collection:
        String indexVariableName = (String) execution.getContext()
                                                     .getVariable(Constants.VAR_INDEX_VARIABLE_NAME);
        StepsUtil.incrementVariable(execution.getContext(), indexVariableName);

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ExecutionWrapper execution) {
        return Messages.ERROR_INCREMENT_INDEX;
    }

}
