package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.function.Function;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("checkAppStep")
public class CheckAppStep extends AbstractXS2ProcessStep {

    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckAppStep.class);

    public static StepMetadata getMetadata() {
        return new StepMetadata("checkAppTask", "Check App", "Check App");
    }

    protected Function<DelegateExecution, CloudFoundryOperations> clientSupplier = (context) -> getCloudFoundryClient(context, LOGGER);

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {

        logActivitiTask(context, LOGGER);

        // Get the next cloud application from the context:
        CloudApplication app = StepsUtil.getApp(context);

        try {
            info(context, format(Messages.CHECKING_APP, app.getName()), LOGGER);

            CloudFoundryOperations client = clientSupplier.apply(context);

            // Check if an application with this name already exists, and store it in the context:
            CloudApplication existingApp = getApplication(client, app.getName());
            StepsUtil.setExistingApp(context, existingApp);

            if (existingApp == null) {
                info(context, format(Messages.APP_DOES_NOT_EXIST, app.getName()), LOGGER);
            } else {
                info(context, format(Messages.APP_EXISTS, app.getName()), LOGGER);
            }

            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, format(Messages.ERROR_CHECKING_APP, app.getName()), e, LOGGER);
            throw e;
        } catch (CloudFoundryException e) {
            SLException ex = StepsUtil.createException(e);
            error(context, format(Messages.ERROR_CHECKING_APP, app.getName()), ex, LOGGER);
            throw ex;
        }
    }

    private CloudApplication getApplication(CloudFoundryOperations client, String appName) {
        try {
            return client.getApplication(appName);
        } catch (CloudFoundryException e) {
            if (!e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                throw e;
            }
        }
        return null;
    }

}
