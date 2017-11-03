package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.function.Supplier;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudTask;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("executeTaskStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ExecuteTaskStep extends AbstractProcessStep {

    protected Supplier<Long> currentTimeSupplier = () -> System.currentTimeMillis();

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_TASKS_INDEX;
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws Exception {
        getStepLogger().logActivitiTask();

        CloudApplicationExtended app = StepsUtil.getApp(context);
        CloudTask taskToExecute = StepsUtil.getTask(context);
        try {
            return attemptToExecuteTask(context, app, taskToExecute);
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            getStepLogger().error(e, Messages.ERROR_EXECUTING_TASK_ON_APP, taskToExecute.getName(), app.getName());
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_EXECUTING_TASK_ON_APP, taskToExecute.getName(), app.getName());
            throw e;
        }
    }

    private ExecutionStatus attemptToExecuteTask(DelegateExecution context, CloudApplication app, CloudTask taskToExecute) {
        ClientExtensions clientExtensions = getClientExtensions(context);

        getStepLogger().info(Messages.EXECUTING_TASK_ON_APP, taskToExecute.getName(), app.getName());
        CloudTask startedTask = runTask(clientExtensions, app, taskToExecute);

        StepsUtil.setStartedTask(context, startedTask);
        context.setVariable(Constants.VAR_START_TIME, currentTimeSupplier.get());
        return ExecutionStatus.SUCCESS;
    }

    private CloudTask runTask(ClientExtensions clientExtensions, CloudApplication app, CloudTask task) {
        return clientExtensions.runTask(app.getName(), task.getName(), task.getCommand(), task.getEnvironmentVariables());
    }

}
