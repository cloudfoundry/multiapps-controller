package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;

@Named("prepareToUndeployStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareToUndeployStep extends SyncFlowableStep {

    protected Function<OperationService, ProcessConflictPreventer> conflictPreventerSupplier = ProcessConflictPreventer::new;
    @Inject
    private OperationService operationService;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        getStepLogger().debug(Messages.DETECTING_COMPONENTS_TO_UNDEPLOY);
        String mtaId = StepsUtil.getRequiredString(execution.getContext(), Constants.PARAM_MTA_ID);

        StepsUtil.setMtaModules(execution.getContext(), getMtaModules(execution.getContext()));
        StepsUtil.setPublishedEntries(execution.getContext(), Collections.emptyList());
        StepsUtil.setModulesToDeploy(execution.getContext(), Collections.emptyList());
        StepsUtil.setAppsToDeploy(execution.getContext(), Collections.emptyList());
        StepsUtil.setAllModulesToDeploy(execution.getContext(), Collections.emptyList());
        StepsUtil.setSubscriptionsToCreate(execution.getContext(), Collections.emptyList());
        execution.getContext()
                 .setVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION, 2);

        conflictPreventerSupplier.apply(operationService)
                                 .acquireLock(mtaId, StepsUtil.getSpaceId(execution.getContext()), execution.getContext()
                                                                                                            .getProcessInstanceId());

        getStepLogger().debug(Messages.COMPONENTS_TO_UNDEPLOY_DETECTED);

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return Messages.ERROR_DETECTING_COMPONENTS_TO_UNDEPLOY;
    }

    private Set<String> getMtaModules(DelegateExecution context) {
        DeployedMta deployedMta = StepsUtil.getDeployedMta(context);
        if (deployedMta == null) {
            return Collections.emptySet();
        }

        return deployedMta.getModules()
                          .stream()
                          .map(DeployedMtaModule::getModuleName)
                          .collect(Collectors.toSet());
    }

}
