package org.cloudfoundry.multiapps.controller.process.util;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

@Named
public class BlueGreenVariablesSetter {

    private ProcessTypeParser processTypeParser;

    @Inject
    public BlueGreenVariablesSetter(ProcessTypeParser processTypeParser) {
        this.processTypeParser = processTypeParser;
    }

    public void set(ProcessContext context) {
        ProcessType processType = getProcessType(context);

        context.setVariable(Variables.SKIP_UPDATE_CONFIGURATION_ENTRIES, ProcessType.BLUE_GREEN_DEPLOY.equals(processType));
        context.setVariable(Variables.SKIP_MANAGE_SERVICE_BROKER, ProcessType.BLUE_GREEN_DEPLOY.equals(processType));
        context.setVariable(Variables.USE_IDLE_URIS, ProcessType.BLUE_GREEN_DEPLOY.equals(processType));
    }

    protected ProcessType getProcessType(ProcessContext context) {
        return processTypeParser.getProcessType(context.getExecution());
    }

}
