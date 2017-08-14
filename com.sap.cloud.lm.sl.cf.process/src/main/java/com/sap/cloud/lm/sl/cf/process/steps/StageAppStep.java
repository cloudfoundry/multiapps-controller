package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.AsyncStepMetadata;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("stageAppStep")
public class StageAppStep extends AbstractXS2ProcessStepWithBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(StageAppStep.class);

    public static StepMetadata getMetadata() {
        return AsyncStepMetadata.builder().id("stageAppTask").displayName("Stage App").description("Stage App").pollTaskId(
            "pollStageAppStatusTask").childrenVisible(true).build();
    }

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_APPS_INDEX;
    }

    @Override
    protected ExecutionStatus pollStatusInternal(DelegateExecution context) {
        logActivitiTask(context, LOGGER);

        CloudApplication app = StepsUtil.getApp(context);
        try {
            return stageApp(context, app);
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            error(context, format(Messages.ERROR_STAGING_APP_1, app.getName()), e, LOGGER);
            throw e;
        }
    }

    private ExecutionStatus stageApp(DelegateExecution context, CloudApplication app) {
        ClientExtensions clientExtensions = getClientExtensions(context, LOGGER);
        info(context, format(Messages.STAGING_APP, app.getName()), LOGGER);
        StartingInfo startingInfo = clientExtensions.stageApplication(app.getName());
        StepsUtil.setStartingInfo(context, startingInfo);
        context.setVariable(Constants.VAR_START_TIME, System.currentTimeMillis());
        context.setVariable(Constants.VAR_OFFSET, 0);

        return ExecutionStatus.SUCCESS;
    }

}
