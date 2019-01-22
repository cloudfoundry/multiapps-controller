package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;
import com.sap.cloud.lm.sl.common.SLException;

@Component("prepareToUndeployStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareToUndeployStep extends SyncFlowableStep {

    @Inject
    private OperationDao operationDao;

    protected Function<OperationDao, ProcessConflictPreventer> conflictPreventerSupplier = dao -> new ProcessConflictPreventer(
        operationDao);

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        getStepLogger().debug(Messages.DETECTING_COMPONENTS_TO_UNDEPLOY);
        try {
            String mtaId = StepsUtil.getRequiredStringParameter(execution.getContext(), Constants.PARAM_MTA_ID);

            StepsUtil.setMtaModules(execution.getContext(), getMtaModules(execution.getContext()));
            StepsUtil.setServiceBrokersToCreate(execution.getContext(), Collections.emptyList());
            StepsUtil.setPublishedEntries(execution.getContext(), Collections.emptyList());
            StepsUtil.setAppsToDeploy(execution.getContext(), Collections.emptyList());
            StepsUtil.setServiceUrlsToRegister(execution.getContext(), Collections.emptyList());
            StepsUtil.setSubscriptionsToCreate(execution.getContext(), Collections.emptyList());

            conflictPreventerSupplier.apply(operationDao)
                .acquireLock(mtaId, StepsUtil.getSpaceId(execution.getContext()), execution.getContext()
                    .getProcessInstanceId());

            getStepLogger().debug(Messages.COMPONENTS_TO_UNDEPLOY_DETECTED);

            return StepPhase.DONE;
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().error(e, Messages.ERROR_DETECTING_COMPONENTS_TO_UNDEPLOY);
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_DETECTING_COMPONENTS_TO_UNDEPLOY);
            throw e;
        }
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
