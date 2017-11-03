package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("stageAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StageAppStep extends AbstractProcessStep {

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_APPS_INDEX;
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) {
        getStepLogger().logActivitiTask();

        CloudApplication app = StepsUtil.getApp(context);
        try {
            return stageApp(context, app);
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            getStepLogger().error(e, Messages.ERROR_STAGING_APP_1, app.getName());
            throw e;
        }
    }

    private ExecutionStatus stageApp(DelegateExecution context, CloudApplication app) {
        ClientExtensions clientExtensions = getClientExtensions(context);
        getStepLogger().info(Messages.STAGING_APP, app.getName());
        StartingInfo startingInfo = clientExtensions.stageApplication(app.getName());
        StepsUtil.setStartingInfo(context, startingInfo);
        context.setVariable(Constants.VAR_START_TIME, System.currentTimeMillis());
        context.setVariable(Constants.VAR_OFFSET, 0);

        return ExecutionStatus.SUCCESS;
    }

}
