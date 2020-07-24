package org.cloudfoundry.multiapps.controller.process.steps;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("incrementIndexStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class IncrementIndexStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        // Continue the iteration over the collection:
        String indexVariableName = context.getVariable(Variables.INDEX_VARIABLE_NAME);
        StepsUtil.incrementVariable(context.getExecution(), indexVariableName);

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_INCREMENT_INDEX;
    }

}
