package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("scaleAppStep")
public class ScaleAppStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScaleAppStep.class);

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("scaleAppTask").displayName("Scale App").description("Scale App").build();
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {

        logActivitiTask(context, LOGGER);

        // Get the next cloud application from the context
        CloudApplication app = StepsUtil.getApp(context);

        // Get the existing application from the context
        CloudApplication existingApp = StepsUtil.getExistingApp(context);

        try {
            info(context, format(Messages.SCALING_APP, app.getName()), LOGGER);

            // Get a cloud foundry client
            CloudFoundryOperations client = getCloudFoundryClient(context, LOGGER);

            // Get application parameters
            String appName = app.getName();
            Integer instances = (app.getInstances() != 0) ? app.getInstances() : null;

            // Update application instances (if needed)
            if (instances != null && (existingApp == null || !instances.equals(existingApp.getInstances()))) {
                debug(context, format("Updating instances of application \"{0}\"", appName), LOGGER);
                client.updateApplicationInstances(appName, instances);
            }

            debug(context, format(Messages.APP_SCALED, app.getName()), LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, format(Messages.ERROR_SCALING_APP, app.getName()), e, LOGGER);
            throw e;
        } catch (CloudFoundryException e) {
            SLException ex = StepsUtil.createException(e);
            error(context, format(Messages.ERROR_SCALING_APP, app.getName()), ex, LOGGER);
            throw ex;
        }
    }

}
