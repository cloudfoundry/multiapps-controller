package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("checkAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CheckAppStep extends AbstractXS2ProcessStep {

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("checkAppTask").displayName("Check App").description("Check App").build();
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {

        getStepLogger().logActivitiTask();

        // Get the next cloud application from the context:
        CloudApplication app = StepsUtil.getApp(context);

        try {
            getStepLogger().info(Messages.CHECKING_APP, app.getName());

            CloudFoundryOperations client = getCloudFoundryClient(context);

            // Check if an application with this name already exists, and store it in the context:
            CloudApplication existingApp = client.getApplication(app.getName(), false);
            StepsUtil.setExistingApp(context, existingApp);

            if (existingApp == null) {
                getStepLogger().info(Messages.APP_DOES_NOT_EXIST, app.getName());
            } else {
                getStepLogger().info(Messages.APP_EXISTS, app.getName());
            }

            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_CHECKING_APP, app.getName());
            throw e;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            getStepLogger().error(e, Messages.ERROR_CHECKING_APP, app.getName());
            throw e;
        }
    }

}
