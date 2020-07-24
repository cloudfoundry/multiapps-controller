package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.UploadToken;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.RestartParameters;
import org.cloudfoundry.multiapps.controller.core.cf.apps.ActionCalculator;
import org.cloudfoundry.multiapps.controller.core.cf.apps.ApplicationStartupState;
import org.cloudfoundry.multiapps.controller.core.cf.apps.ApplicationStartupStateCalculator;
import org.cloudfoundry.multiapps.controller.core.cf.apps.ApplicationStateAction;
import org.cloudfoundry.multiapps.controller.core.cf.apps.ChangedApplicationActionCalculator;
import org.cloudfoundry.multiapps.controller.core.cf.apps.UnchangedApplicationActionCalculator;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("determineDesiredStateAchievingActionsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetermineDesiredStateAchievingActionsStep extends SyncFlowableStep {

    @Inject
    private ApplicationStartupStateCalculator startupStateCalculator;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        String appName = context.getVariable(Variables.APP_TO_PROCESS)
                                .getName();
        CloudControllerClient client = context.getControllerClient();
        CloudApplication app = client.getApplication(appName);
        ApplicationStartupState currentState = startupStateCalculator.computeCurrentState(app);
        getStepLogger().debug(Messages.CURRENT_STATE, appName, currentState);
        ApplicationStartupState desiredState = computeDesiredState(context, app);
        getStepLogger().debug(Messages.DESIRED_STATE, appName, desiredState);
        UploadToken uploadToken = context.getVariable(Variables.UPLOAD_TOKEN);
        boolean appHasUnstagedContent = uploadToken != null;
        Set<ApplicationStateAction> actionsToExecute = getActionsCalculator(context).determineActionsToExecute(currentState, desiredState,
                                                                                                               !appHasUnstagedContent);
        getStepLogger().debug(Messages.ACTIONS_TO_EXECUTE, appName, actionsToExecute);

        context.setVariable(Variables.APP_STATE_ACTIONS_TO_EXECUTE, new ArrayList<>(actionsToExecute));
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_DETERMINING_ACTIONS_TO_EXECUTE_ON_APP, context.getVariable(Variables.APP_TO_PROCESS)
                                                                                                 .getName());
    }

    private ApplicationStartupState computeDesiredState(ProcessContext context, CloudApplication app) {
        boolean shouldNotStartAnyApp = context.getVariable(Variables.NO_START);
        return startupStateCalculator.computeDesiredState(app, shouldNotStartAnyApp);
    }

    private ActionCalculator getActionsCalculator(ProcessContext context) {
        boolean shouldRestartApp = determineAppRestart(context);
        return shouldRestartApp ? new ChangedApplicationActionCalculator() : new UnchangedApplicationActionCalculator();
    }

    private boolean determineAppRestart(ProcessContext context) {
        boolean appContentChanged = context.getVariable(Variables.APP_CONTENT_CHANGED);
        if (appContentChanged) {
            return true;
        }
        boolean appPropertiesChanged = context.getVariable(Variables.VCAP_APP_PROPERTIES_CHANGED);
        boolean servicesPropertiesChanged = context.getVariable(Variables.VCAP_SERVICES_PROPERTIES_CHANGED);
        boolean userPropertiesChanged = context.getVariable(Variables.USER_PROPERTIES_CHANGED);

        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        RestartParameters restartParameters = app.getRestartParameters();

        if (restartParameters.getShouldRestartOnVcapAppChange() && appPropertiesChanged) {
            return true;
        }
        if (restartParameters.getShouldRestartOnVcapServicesChange() && servicesPropertiesChanged) {
            return true;
        }
        return restartParameters.getShouldRestartOnUserProvidedChange() && userPropertiesChanged;
    }

}
