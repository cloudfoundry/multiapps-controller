package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Set;
import java.util.function.Supplier;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
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
public class DetermineDesiredStateAchievingActionsStep extends AbstractProcessStep {

    protected Supplier<ApplicationStartupStateCalculator> appStateCalculatorSupplier = () -> new ApplicationStartupStateCalculator();

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) {
        getStepLogger().logActivitiTask();
        CloudApplication app = StepsUtil.getApp(context);
        try {
            return attemptToExecuteStep(context);
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            getStepLogger().error(e, Messages.ERROR_DETERMINING_ACTIONS_TO_EXECUTE_ON_APP, app.getName());
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_DETERMINING_ACTIONS_TO_EXECUTE_ON_APP, app.getName());
            throw e;
        }
    }

    private ExecutionStatus attemptToExecuteStep(DelegateExecution context) {
        CloudApplication app = StepsUtil.getApp(context);
        ApplicationStartupState currentState = computeCurrentState(context, app);
        getStepLogger().debug(Messages.CURRENT_STATE, app.getName(), currentState);
        ApplicationStartupState desiredState = computeDesiredState(context, app);
        getStepLogger().debug(Messages.DESIRED_STATE, app.getName(), desiredState);

        Set<ApplicationStateAction> actionsToExecute = getActionsCalculator(context).determineActionsToExecute(currentState, desiredState);
        getStepLogger().debug(Messages.ACTIONS_TO_EXECUTE, app.getName(), actionsToExecute);

        StepsUtil.setAppStateActionsToExecute(context, actionsToExecute);
        return ExecutionStatus.SUCCESS;
    }

    private ApplicationStartupState computeCurrentState(DelegateExecution context, CloudApplication app) {
        CloudFoundryOperations client = getCloudFoundryClient(context);
        return appStateCalculatorSupplier.get().computeCurrentState(client.getApplication(app.getName()));
    }

    private ApplicationStartupState computeDesiredState(DelegateExecution context, CloudApplication app) {
        boolean shouldNotStartAnyApp = (boolean) context.getVariable(Constants.PARAM_NO_START);
        return appStateCalculatorSupplier.get().computeDesiredState(app, shouldNotStartAnyApp);
    }

    private ActionCalculator getActionsCalculator(DelegateExecution context) {
        String hasAppChangedString = (String) context.getVariable(Constants.VAR_HAS_APP_CHANGED);
        return Boolean.valueOf(hasAppChangedString) ? new ChangedApplicationActionCalcultor() : new UnchangedApplicationActionCalculator();
    }

}
