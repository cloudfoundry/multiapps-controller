package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Set;
import java.util.function.Supplier;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("determineDesiredStateAchievingActionsStep")
public class DetermineDesiredStateAchievingActionsStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(DetermineDesiredStateAchievingActionsStep.class);

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("determineDesiredStateAchievingActionsTask").displayName(
            "Determine Desired State Achieving Actions").description("Determine Desired State Achieving Actions").build();
    }

    protected Supplier<ApplicationStartupStateCalculator> appStateCalculatorSupplier = () -> new ApplicationStartupStateCalculator();

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) {
        logActivitiTask(context, LOGGER);
        CloudApplication app = StepsUtil.getApp(context);
        try {
            return attemptToExecuteStep(context);
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            error(context, MessageFormat.format(Messages.ERROR_DETERMINING_ACTIONS_TO_EXECUTE_ON_APP, app.getName()), LOGGER);
            throw e;
        } catch (SLException e) {
            error(context, MessageFormat.format(Messages.ERROR_DETERMINING_ACTIONS_TO_EXECUTE_ON_APP, app.getName()), LOGGER);
            throw e;
        }
    }

    private ExecutionStatus attemptToExecuteStep(DelegateExecution context) {
        CloudApplication app = StepsUtil.getApp(context);
        ApplicationStartupState currentState = computeCurrentState(context, app);
        debug(context, MessageFormat.format(Messages.CURRENT_STATE, app.getName(), currentState), LOGGER);
        ApplicationStartupState desiredState = computeDesiredState(context, app);
        debug(context, MessageFormat.format(Messages.DESIRED_STATE, app.getName(), desiredState), LOGGER);

        Set<ApplicationStateAction> actionsToExecute = getActionsCalculator(context).determineActionsToExecute(currentState, desiredState);
        debug(context, MessageFormat.format(Messages.ACTIONS_TO_EXECUTE, app.getName(), actionsToExecute), LOGGER);

        StepsUtil.setAppStateActionsToExecute(context, actionsToExecute);
        return ExecutionStatus.SUCCESS;
    }

    private ApplicationStartupState computeCurrentState(DelegateExecution context, CloudApplication app) {
        CloudFoundryOperations client = getCloudFoundryClient(context, LOGGER);
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
