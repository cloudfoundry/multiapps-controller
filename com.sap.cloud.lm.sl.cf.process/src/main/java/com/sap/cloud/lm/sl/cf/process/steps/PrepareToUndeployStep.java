package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.function.Function;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.dao.OngoingOperationDao;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("prepareToUndeployStep")
public class PrepareToUndeployStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareToUndeployStep.class);

    public static StepMetadata getMetadata() {
        return new StepMetadata("prepareToUndeployTask", "Prepare Undeploy", "Prepare Undeploy");
    }

    @Inject
    private OngoingOperationDao ongoingOperationDao;

    protected Function<OngoingOperationDao, ProcessConflictPreventer> conflictPreventerSupplier = (dao) -> new ProcessConflictPreventer(
        ongoingOperationDao);

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);
        info(context, Messages.DETECTING_COMPONENTS_TO_UNDEPLOY, LOGGER);
        try {
            String mtaId = StepsUtil.getRequiredStringParameter(context, Constants.PARAM_MTA_ID);

            StepsUtil.setMtaModules(context, Collections.emptySet());
            StepsUtil.setServiceBrokersToCreate(context, Collections.emptyList());
            StepsUtil.setPublishedEntries(context, Collections.emptyList());
            StepsUtil.setDependenciesToPublish(context, Collections.emptyList());
            StepsUtil.setAppsToDeploy(context, Collections.emptyList());
            StepsUtil.setServiceUrlsToRegister(context, Collections.emptyList());
            StepsUtil.setSubscriptionsToCreate(context, Collections.emptyList());

            conflictPreventerSupplier.apply(ongoingOperationDao).attemptToAcquireLock(mtaId, StepsUtil.getSpaceId(context),
                context.getProcessInstanceId());

            debug(context, Messages.COMPONENTS_TO_UNDEPLOY_DETECTED, LOGGER);

            return ExecutionStatus.SUCCESS;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            error(context, Messages.ERROR_DETECTING_COMPONENTS_TO_UNDEPLOY, e, LOGGER);
            throw e;
        } catch (SLException e) {
            error(context, Messages.ERROR_DETECTING_COMPONENTS_TO_UNDEPLOY, e, LOGGER);
            throw e;
        }
    }

}
