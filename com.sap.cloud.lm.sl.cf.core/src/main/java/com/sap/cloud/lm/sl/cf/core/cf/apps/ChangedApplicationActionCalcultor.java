package com.sap.cloud.lm.sl.cf.core.cf.apps;

import java.util.HashSet;
import java.util.Set;

public class ChangedApplicationActionCalcultor implements ActionCalculator {

    @Override
    public Set<ApplicationStateAction> determineActionsToExecute(ApplicationStartupState currentState,
                                                                 ApplicationStartupState desiredState) {
        Set<ApplicationStateAction> result = new HashSet<>();
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
