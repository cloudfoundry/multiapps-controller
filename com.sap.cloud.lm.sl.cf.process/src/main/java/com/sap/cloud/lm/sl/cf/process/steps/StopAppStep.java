package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.function.Function;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("stopAppStep")
public class StopAppStep extends AbstractXS2ProcessStep {

    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(StopAppStep.class);

    public static StepMetadata getMetadata() {
        return new StepMetadata("stopAppTask", "Stop App", "Stop App");
    }

    protected Function<DelegateExecution, CloudFoundryOperations> clientSupplier = (context) -> getCloudFoundryClient(context, LOGGER);

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {

        logActivitiTask(context, LOGGER);

        // Get the next cloud application from the context
        CloudApplication app = StepsUtil.getApp(context);

        // Get the existing application from the context
        CloudApplication existingApp = StepsUtil.getExistingApp(context);

        try {
            if (existingApp != null && !existingApp.getState().equals(AppState.STOPPED)) {
                info(context, format(Messages.STOPPING_APP, app.getName()), LOGGER);

                // Get a cloud foundry client
                CloudFoundryOperations client = clientSupplier.apply(context);

                // Stop the application
                client.stopApplication(app.getName());

                debug(context, format(Messages.APP_STOPPED, app.getName()), LOGGER);
            } else {
                debug(context, format("Application \"{0}\" already stopped", app.getName()), LOGGER);
            }

            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, format(Messages.ERROR_STOPPING_APP, app.getName()), e, LOGGER);
            throw e;
        } catch (CloudFoundryException e) {
            SLException ex = StepsUtil.createException(e);
            error(context, format(Messages.ERROR_STOPPING_APP, app.getName()), ex, LOGGER);
            throw ex;
        }
    }

}
