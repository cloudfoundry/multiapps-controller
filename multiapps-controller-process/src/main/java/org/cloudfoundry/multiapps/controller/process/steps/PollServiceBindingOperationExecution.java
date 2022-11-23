package org.cloudfoundry.multiapps.controller.process.steps;

import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public class PollServiceBindingOperationExecution extends PollServiceBindingUnbindingOperationBaseExecution {

    @Override
    protected String getAsyncJobId(ProcessContext context) {
        return context.getVariable(Variables.SERVICE_BINDING_JOB_ID);
    }

}
