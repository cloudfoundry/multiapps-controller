package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ProcessConflictPreventer;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("prepareToUndeployStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareToUndeployStep extends SyncFlowableStep {

    protected Function<OperationService, ProcessConflictPreventer> conflictPreventerSupplier = ProcessConflictPreventer::new;
    @Inject
    private OperationService operationService;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.DETECTING_COMPONENTS_TO_UNDEPLOY);
        String mtaId = context.getRequiredVariable(Variables.MTA_ID);
        String namespace = context.getVariable(Variables.MTA_NAMESPACE);

        context.setVariable(Variables.MTA_MODULES, getMtaModules(context));
        context.setVariable(Variables.PUBLISHED_ENTRIES, Collections.emptyList());
        context.setVariable(Variables.SERVICES_TO_CREATE, Collections.emptyList());
        context.setVariable(Variables.MODULES_TO_DEPLOY, Collections.emptyList());
        context.setVariable(Variables.APPS_TO_DEPLOY, Collections.emptyList());
        context.setVariable(Variables.ALL_MODULES_TO_DEPLOY, Collections.emptyList());
        context.setVariable(Variables.SUBSCRIPTIONS_TO_CREATE, Collections.emptyList());
        context.setVariable(Variables.MTA_MAJOR_SCHEMA_VERSION, 2);

        conflictPreventerSupplier.apply(operationService)
                                 .acquireLock(mtaId, namespace, context.getVariable(Variables.SPACE_GUID), context.getExecution()
                                                                                                                  .getProcessInstanceId());

        getStepLogger().debug(Messages.COMPONENTS_TO_UNDEPLOY_DETECTED);

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_DETECTING_COMPONENTS_TO_UNDEPLOY;
    }

    private Set<String> getMtaModules(ProcessContext context) {
        DeployedMta deployedMta = context.getVariable(Variables.DEPLOYED_MTA);
        if (deployedMta == null) {
            return Collections.emptySet();
        }

        return deployedMta.getApplications()
                          .stream()
                          .map(DeployedMtaApplication::getModuleName)
                          .collect(Collectors.toSet());
    }

}
