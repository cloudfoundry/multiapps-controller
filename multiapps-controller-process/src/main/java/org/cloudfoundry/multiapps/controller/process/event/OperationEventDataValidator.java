package org.cloudfoundry.multiapps.controller.process.event;

import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;

public class OperationEventDataValidator {
    
    public static boolean requiredDataExist(DelegateExecution execution) {
        return VariableHandling.get(execution, Variables.ORGANIZATION_GUID) == null
            || VariableHandling.get(execution, Variables.MTA_ID) == null;
    }
    
}
