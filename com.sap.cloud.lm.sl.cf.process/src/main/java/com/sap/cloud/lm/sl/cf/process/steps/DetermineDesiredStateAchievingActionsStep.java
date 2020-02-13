package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Set;
import java.util.function.Supplier;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.RestartParameters;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ActionCalculator;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStartupState;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStartupStateCalculator;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStateAction;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ChangedApplicationActionCalculator;
import com.sap.cloud.lm.sl.cf.core.cf.apps.UnchangedApplicationActionCalculator;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationStager;

@Named("determineDesiredStateAchievingActionsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetermineDesiredStateAchievingActionsStep extends SyncFlowableStep {

    protected Supplier<ApplicationStartupStateCalculator> appStateCalculatorSupplier = ApplicationStartupStateCalculator::new;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        String appName = StepsUtil.getApp(execution.getContext())
                                  .getName();
        CloudControllerClient client = execution.getControllerClient();
        CloudApplication app = client.getApplication(appName);
        ApplicationStartupState currentState = computeCurrentState(app);
        getStepLogger().debug(Messages.CURRENT_STATE, appName, currentState);
        ApplicationStartupState desiredState = computeDesiredState(execution.getContext(), app);
        getStepLogger().debug(Messages.DESIRED_STATE, appName, desiredState);
        ApplicationStager applicationStager = new ApplicationStager(execution.getControllerClient());
        Set<ApplicationStateAction> actionsToExecute = getActionsCalculator(execution.getContext()).determineActionsToExecute(currentState,
                                                                                                                              desiredState,
                                                                                                                              applicationStager.isApplicationStagedCorrectly(execution.getStepLogger(),
                                                                                                                                                                             app));
        getStepLogger().debug(Messages.ACTIONS_TO_EXECUTE, appName, actionsToExecute);

        StepsUtil.setAppStateActionsToExecute(execution.getContext(), actionsToExecute);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return MessageFormat.format(Messages.ERROR_DETERMINING_ACTIONS_TO_EXECUTE_ON_APP, StepsUtil.getApp(context)
                                                                                                   .getName());
    }

    private ApplicationStartupState computeCurrentState(CloudApplication app) {
        return appStateCalculatorSupplier.get()
                                         .computeCurrentState(app);
    }

    private ApplicationStartupState computeDesiredState(DelegateExecution context, CloudApplication app) {
        boolean shouldNotStartAnyApp = (boolean) context.getVariable(Constants.PARAM_NO_START);
        return appStateCalculatorSupplier.get()
                                         .computeDesiredState(app, shouldNotStartAnyApp);
    }

    private ActionCalculator getActionsCalculator(DelegateExecution context) {
        boolean shouldRestartApp = determineAppRestart(context);
        return shouldRestartApp ? new ChangedApplicationActionCalculator() : new UnchangedApplicationActionCalculator();
    }

    private boolean determineAppRestart(DelegateExecution context) {
        String appContentChangedString = StepsUtil.getString(context, Constants.VAR_APP_CONTENT_CHANGED, Boolean.toString(false));
        if (Boolean.parseBoolean(appContentChangedString)) {
            return true;
        }
        boolean appPropertiesChanged = StepsUtil.getVcapAppPropertiesChanged(context);
        boolean servicesPropertiesChanged = StepsUtil.getVcapServicesPropertiesChanged(context);
        boolean userPropertiesChanged = StepsUtil.getUserPropertiesChanged(context);

        CloudApplicationExtended app = StepsUtil.getApp(context);
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
