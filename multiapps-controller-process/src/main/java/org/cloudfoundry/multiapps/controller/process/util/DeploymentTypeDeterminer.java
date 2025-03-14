package org.cloudfoundry.multiapps.controller.process.util;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;

@Named
public class DeploymentTypeDeterminer {

    protected final ProcessTypeParser processTypeParser;

    @Inject
    public DeploymentTypeDeterminer(ProcessTypeParser processTypeParser) {
        this.processTypeParser = processTypeParser;
    }

    public ProcessType determineDeploymentType(ProcessContext context) {
        return processTypeParser.getProcessType(context.getExecution());
    }

}
