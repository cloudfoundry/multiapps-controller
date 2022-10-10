package org.cloudfoundry.multiapps.controller.core.cf.apps;

import java.util.EnumSet;
import java.util.Set;

public class ChangedApplicationActionCalculator implements ActionCalculator {

    @Override
    public Set<ApplicationStateAction> determineActionsToExecute(ApplicationStartupState currentState, ApplicationStartupState desiredState,
                                                                 boolean isApplicationStagedCorrectly) {
        Set<ApplicationStateAction> result = EnumSet.noneOf(ApplicationStateAction.class);
        if (desiredState.equals(ApplicationStartupState.STARTED)) {
            result.add(ApplicationStateAction.START);
        }
        if (currentState.equals(ApplicationStartupState.STARTED) || currentState.equals(ApplicationStartupState.INCONSISTENT)) {
            result.add(ApplicationStateAction.STOP);
        }
        if (desiredState.equals(ApplicationStartupState.EXECUTED)) {
            result.add(ApplicationStateAction.START);
            result.add(ApplicationStateAction.EXECUTE);
        }
        result.add(ApplicationStateAction.STAGE);
        return result;
    }

}
