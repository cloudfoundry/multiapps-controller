package com.sap.cloud.lm.sl.cf.core.cf.apps;

import java.util.Set;

public interface ActionCalculator {

    Set<ApplicationStateAction> determineActionsToExecute(ApplicationStartupState currentState, ApplicationStartupState desiredState, boolean isApplicationStagedCorrectly);

}
