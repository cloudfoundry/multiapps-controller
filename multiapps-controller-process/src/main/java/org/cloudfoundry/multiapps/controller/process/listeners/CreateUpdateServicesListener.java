package org.cloudfoundry.multiapps.controller.process.listeners;

import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.steps.StepsUtil;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;

@Named("createUpdateServicesListener")
public class CreateUpdateServicesListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;

    @Override
    protected void notifyInternal(DelegateExecution execution) throws Exception {
        List<CloudServiceInstanceExtended> services = VariableHandling.get(execution, Variables.SERVICES_TO_CREATE);

        services.stream()
                .map(CloudServiceInstanceExtended::getName)
                .map(DetermineServiceCreateUpdateActionsListener::buildExportedVariableName)
                .forEach(variableName -> setVariableInParentProcess(execution, variableName, StepsUtil.getObject(execution, variableName)));
    }

}
