package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.function.Function;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;
import com.sap.cloud.lm.sl.common.SLException;

@Component("prepareToUndeployStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareToUndeployStep extends SyncActivitiStep {

    @Inject
    private OperationDao operationDao;

    protected Function<OperationDao, ProcessConflictPreventer> conflictPreventerSupplier = (dao) -> new ProcessConflictPreventer(
        operationDao);

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws SLException {
        getStepLogger().logActivitiTask();
        getStepLogger().info(Messages.DETECTING_COMPONENTS_TO_UNDEPLOY);
        try {
            String mtaId = StepsUtil.getRequiredStringParameter(execution.getContext(), Constants.PARAM_MTA_ID);

            StepsUtil.setMtaModules(execution.getContext(), Collections.emptySet());
            StepsUtil.setServiceBrokersToCreate(execution.getContext(), Collections.emptyList());
            StepsUtil.setPublishedEntries(execution.getContext(), Collections.emptyList());
            StepsUtil.setConfigurationEntriesToPublish(execution.getContext(), Collections.emptyMap());
            StepsUtil.setAppsToDeploy(execution.getContext(), Collections.emptyList());
            StepsUtil.setServiceUrlsToRegister(execution.getContext(), Collections.emptyList());
            StepsUtil.setSubscriptionsToCreate(execution.getContext(), Collections.emptyList());

            conflictPreventerSupplier.apply(operationDao)
                .attemptToAcquireLock(mtaId, StepsUtil.getSpaceId(execution.getContext()), execution.getContext()
                    .getProcessInstanceId());

            getStepLogger().debug(Messages.COMPONENTS_TO_UNDEPLOY_DETECTED);

            return StepPhase.DONE;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            getStepLogger().error(e, Messages.ERROR_DETECTING_COMPONENTS_TO_UNDEPLOY);
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_DETECTING_COMPONENTS_TO_UNDEPLOY);
            throw e;
        }
    }

}
