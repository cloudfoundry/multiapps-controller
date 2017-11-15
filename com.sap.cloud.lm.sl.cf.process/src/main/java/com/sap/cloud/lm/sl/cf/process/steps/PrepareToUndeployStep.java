package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.function.Function;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;
import com.sap.cloud.lm.sl.common.SLException;

@Component("prepareToUndeployStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareToUndeployStep extends AbstractProcessStep {

    @Inject
    private OperationDao operationDao;

    protected Function<OperationDao, ProcessConflictPreventer> conflictPreventerSupplier = (dao) -> new ProcessConflictPreventer(
        operationDao);

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        getStepLogger().logActivitiTask();
        getStepLogger().info(Messages.DETECTING_COMPONENTS_TO_UNDEPLOY);
        try {
            String mtaId = StepsUtil.getRequiredStringParameter(context, Constants.PARAM_MTA_ID);

            StepsUtil.setMtaModules(context, Collections.emptySet());
            StepsUtil.setServiceBrokersToCreate(context, Collections.emptyList());
            StepsUtil.setPublishedEntries(context, Collections.emptyList());
            StepsUtil.setConfigurationEntriesToPublish(context, Collections.emptyMap());
            StepsUtil.setAppsToDeploy(context, Collections.emptyList());
            StepsUtil.setServiceUrlsToRegister(context, Collections.emptyList());
            StepsUtil.setSubscriptionsToCreate(context, Collections.emptyList());

            conflictPreventerSupplier.apply(operationDao).attemptToAcquireLock(mtaId, StepsUtil.getSpaceId(context),
                context.getProcessInstanceId());

            getStepLogger().debug(Messages.COMPONENTS_TO_UNDEPLOY_DETECTED);

            return ExecutionStatus.SUCCESS;
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
