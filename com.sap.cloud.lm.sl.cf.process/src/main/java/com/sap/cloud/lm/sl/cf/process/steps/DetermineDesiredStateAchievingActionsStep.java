package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Map;
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
import com.sap.cloud.lm.sl.cf.core.cf.apps.ActionCalculator;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStartupState;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStartupStateCalculator;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStateAction;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ChangedApplicationActionCalcultor;
import com.sap.cloud.lm.sl.cf.core.cf.apps.UnchangedApplicationActionCalculator;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
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
        boolean hasAppChanged = determineHasAppChanged(context);
        boolean shouldRestartOnEnvChange = determineAppRestart(context);
        return hasAppChanged || shouldRestartOnEnvChange ? new ChangedApplicationActionCalcultor()
            : new UnchangedApplicationActionCalculator();
    }

    protected boolean determineHasAppChanged(DelegateExecution context) {
        String appContentChangedString = StepsUtil.getVariableOrDefault(context, Constants.VAR_APP_CONTENT_CHANGED,
            Boolean.toString(false));
        return Boolean.valueOf(appContentChangedString);
    }

    private boolean determineAppRestart(DelegateExecution context) {
        boolean appPropertiesChanged = StepsUtil.getVcapAppPropertiesChanged(context);
        boolean servicesPropertiesChanged = StepsUtil.getVcapServicesPropertiesChanged(context);
        boolean userPropertiesChanged = StepsUtil.getUserPropertiesChanged(context);

        CloudApplicationExtended app = StepsUtil.getApp(context);
        Map<String, Boolean> appRestartParameters = app.getRestartParameters();

        if (appRestartParameters.get(SupportedParameters.VCAP_APPLICATION_ENV) && appPropertiesChanged) {
            return true;
        }
        if (appRestartParameters.get(SupportedParameters.VCAP_SERVICES_ENV) && servicesPropertiesChanged) {
            return true;
        }
        if (appRestartParameters.get(SupportedParameters.USER_PROVIDED_ENV) && userPropertiesChanged) {
            return true;
        }
        return false;
    }

}
