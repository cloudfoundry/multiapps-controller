package org.cloudfoundry.multiapps.controller.core.cf.apps;

import java.util.Set;

public interface ActionCalculator {

    Set<ApplicationStateAction> determineActionsToExecute(ApplicationStartupState currentState, ApplicationStartupState desiredState,
                                                          boolean isApplicationStagedCorrectly);

}
