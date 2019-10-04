package com.sap.cloud.lm.sl.cf.process.util;

import org.flowable.engine.HistoryService;
import org.flowable.variable.api.history.HistoricVariableInstance;

import com.sap.cloud.lm.sl.cf.process.Constants;

public class HistoricVariablesUtil {

    private HistoricVariablesUtil() {
    }

    public static String getCurrentUser(HistoryService historyService, String processInstanceId) {
        HistoricVariableInstance userVariable = getHistoricVarInstanceValue(historyService, processInstanceId, Constants.VAR_USER);
        return userVariable != null ? (String) userVariable.getValue() : null;
    }

    public static HistoricVariableInstance getHistoricVarInstanceValue(HistoryService historyService, String processInstanceId,
                                                                       String parameter) {
        return historyService.createHistoricVariableInstanceQuery()
                             .processInstanceId(processInstanceId)
                             .variableName(parameter)
                             .singleResult();
    }

}
