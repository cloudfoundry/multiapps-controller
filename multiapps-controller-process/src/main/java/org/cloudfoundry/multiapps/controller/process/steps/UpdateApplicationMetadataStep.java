package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

@Named("updateApplicationMetadataStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateApplicationMetadataStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);

        updateApplicationMetadata(context, app);
        getStepLogger().debug(Messages.UPDATED_METADATA_APPLICATION, app.getName());

        return StepPhase.DONE;
    }

    private void updateApplicationMetadata(ProcessContext context, CloudApplicationExtended app) {
        CloudControllerClient client = context.getControllerClient();
        CloudApplication existingApp = client.getApplication(app.getName());

        if (isNewApplication(context)) {
            addApplicationMetadata(client, app, existingApp);
            return;
        }
        updateExistingApplicationMetadata(client, app, existingApp);
    }

    private boolean isNewApplication(ProcessContext context) {
        return context.getVariable(Variables.EXISTING_APP) == null;
    }

    private void addApplicationMetadata(CloudControllerClient client, CloudApplicationExtended app, CloudApplication existingApp) {
        client.updateApplicationMetadata(existingApp.getMetadata()
                                                    .getGuid(),
                                         app.getV3Metadata());
    }

    private void updateExistingApplicationMetadata(CloudControllerClient client, CloudApplicationExtended app,
                                                   CloudApplication existingApp) {
        if (app.getV3Metadata() == null) {
            return;
        }
        boolean shouldUpdateMetadata = true;
        if (existingApp.getV3Metadata() != null) {
            shouldUpdateMetadata = !existingApp.getV3Metadata()
                                               .equals(app.getV3Metadata());
        }
        if (shouldUpdateMetadata) {
            client.updateApplicationMetadata(existingApp.getMetadata()
                                                        .getGuid(),
                                             app.getV3Metadata());
        }
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_UPDATING_METADATA_OF_APPLICATION_0, context.getVariable(Variables.APP_TO_PROCESS)
                                                                                              .getName());
    }

}
