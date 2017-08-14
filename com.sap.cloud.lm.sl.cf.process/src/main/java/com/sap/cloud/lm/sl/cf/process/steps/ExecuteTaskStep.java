package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.text.MessageFormat;
import java.util.function.Supplier;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudTask;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.AsyncStepMetadata;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("executeTaskStep")
public class ExecuteTaskStep extends AbstractXS2ProcessStepWithBridge {

    static final Logger LOGGER = LoggerFactory.getLogger(ExecuteTaskStep.class);

    public static StepMetadata getMetadata() {
        return AsyncStepMetadata.builder().id("executeTaskTask").displayName("Execute Task").description("Execute Task").pollTaskId(
            "pollExecuteTaskStatusTask").childrenVisible(true).build();
    }

    protected Supplier<Long> currentTimeSupplier = () -> System.currentTimeMillis();

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_TASKS_INDEX;
    }

    @Override
    protected ExecutionStatus pollStatusInternal(DelegateExecution context) throws Exception {
        logActivitiTask(context, LOGGER);

        CloudApplicationExtended app = StepsUtil.getApp(context);
        CloudTask taskToExecute = StepsUtil.getTask(context);
        try {
            return attemptToExecuteTask(context, app, taskToExecute);
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            error(context, format(Messages.ERROR_EXECUTING_TASK_ON_APP, taskToExecute.getName(), app.getName()), e, LOGGER);
            throw e;
        } catch (SLException e) {
            error(context, format(Messages.ERROR_EXECUTING_TASK_ON_APP, taskToExecute.getName(), app.getName()), e, LOGGER);
            throw e;
        }
    }

    private ExecutionStatus attemptToExecuteTask(DelegateExecution context, CloudApplication app, CloudTask taskToExecute) {
        ClientExtensions clientExtensions = getClientExtensions(context, LOGGER);

        info(context, MessageFormat.format(Messages.EXECUTING_TASK_ON_APP, taskToExecute.getName(), app.getName()), LOGGER);
        CloudTask startedTask = runTask(clientExtensions, app, taskToExecute);

        StepsUtil.setStartedTask(context, startedTask);
        context.setVariable(Constants.VAR_START_TIME, currentTimeSupplier.get());
        return ExecutionStatus.SUCCESS;
    }

    private CloudTask runTask(ClientExtensions clientExtensions, CloudApplication app, CloudTask task) {
        return clientExtensions.runTask(app.getName(), task.getName(), task.getCommand(), task.getEnvironmentVariables());
    }

}
