package com.sap.cloud.lm.sl.cf.core.cf.apps;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import com.sap.cloud.lm.sl.cf.core.message.Messages;

public class UnchangedApplicationActionCalculator implements ActionCalculator {

    @Override
    public Set<ApplicationStateAction> determineActionsToExecute(ApplicationStartupState currentState, ApplicationStartupState desiredState) {
        Set<ApplicationStateAction> actionsToExecute = new HashSet<>();
        if (currentState.equals(desiredState)) {
            return actionsToExecute;
        }
        switch (desiredState) {
            case STARTED:
                if (currentState.equals(ApplicationStartupState.INCONSISTENT)) {
                    actionsToExecute.add(ApplicationStateAction.STOP);
                }
                actionsToExecute.add(ApplicationStateAction.STAGE);
                actionsToExecute.add(ApplicationStateAction.START);
                return actionsToExecute;
            case STOPPED:
                actionsToExecute.add(ApplicationStateAction.STOP);
                return actionsToExecute;
            default:
                throw new IllegalStateException(MessageFormat.format(Messages.ILLEGAL_DESIRED_STATE, desiredState));
        }
    }

}
