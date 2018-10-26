package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Set;
import java.util.function.Supplier;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.RestartParameters;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ActionCalculator;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStartupState;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStartupStateCalculator;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStateAction;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ChangedApplicationActionCalcultor;
import com.sap.cloud.lm.sl.cf.core.cf.apps.UnchangedApplicationActionCalculator;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("determineDesiredStateAchievingActionsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetermineDesiredStateAchievingActionsStep extends SyncActivitiStep {

    protected Supplier<ApplicationStartupStateCalculator> appStateCalculatorSupplier = ApplicationStartupStateCalculator::new;

    @Override
    protected StepPhase executeStep(ExecutionWrapper context) {
        CloudApplication app = StepsUtil.getApp(context.getContext());
        try {
            return attemptToExecuteStep(context);
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().error(e, Messages.ERROR_DETERMINING_ACTIONS_TO_EXECUTE_ON_APP, app.getName());
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_DETERMINING_ACTIONS_TO_EXECUTE_ON_APP, app.getName());
            throw e;
        }
    }

    private StepPhase attemptToExecuteStep(ExecutionWrapper execution) {
        CloudApplication app = StepsUtil.getApp(execution.getContext());
        ApplicationStartupState currentState = computeCurrentState(execution, app);
        getStepLogger().debug(Messages.CURRENT_STATE, app.getName(), currentState);
        ApplicationStartupState desiredState = computeDesiredState(execution.getContext(), app);
        getStepLogger().debug(Messages.DESIRED_STATE, app.getName(), desiredState);

        Set<ApplicationStateAction> actionsToExecute = getActionsCalculator(execution.getContext()).determineActionsToExecute(currentState,
            desiredState);
        getStepLogger().debug(Messages.ACTIONS_TO_EXECUTE, app.getName(), actionsToExecute);

        StepsUtil.setAppStateActionsToExecute(execution.getContext(), actionsToExecute);
        return StepPhase.DONE;
    }

    private ApplicationStartupState computeCurrentState(ExecutionWrapper execution, CloudApplication app) {
        CloudControllerClient client = execution.getControllerClient();
        return appStateCalculatorSupplier.get()
            .computeCurrentState(client.getApplication(app.getName()));
    }

    private ApplicationStartupState computeDesiredState(DelegateExecution context, CloudApplication app) {
        boolean shouldNotStartAnyApp = (boolean) context.getVariable(Constants.PARAM_NO_START);
        return appStateCalculatorSupplier.get()
            .computeDesiredState(app, shouldNotStartAnyApp);
    }

    private ActionCalculator getActionsCalculator(DelegateExecution context) {
        boolean shouldRestartApp = determineAppRestart(context);
        return shouldRestartApp ? new ChangedApplicationActionCalcultor() : new UnchangedApplicationActionCalculator();
    }

    private boolean determineAppRestart(DelegateExecution context) {
        String appContentChangedString = StepsUtil.getVariableOrDefault(context, Constants.VAR_APP_CONTENT_CHANGED,
            Boolean.toString(false));
        if (Boolean.valueOf(appContentChangedString)) {
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
        if (restartParameters.getShouldRestartOnUserProvidedChange() && userPropertiesChanged) {
            return true;
        }
        return false;
    }

}
