package com.sap.cloud.lm.sl.cf.process.util;

import com.sap.cloud.lm.sl.cf.process.Constants;
import org.flowable.engine.HistoryService;
import org.flowable.variable.api.history.HistoricVariableInstance;

public class HistoricVariablesUtil {

    private HistoricVariablesUtil() {
    }

    public static String getCurrentUser(HistoryService historyService, String processInstanceId) {
        String user = (String) getHistoricVarInstanceValue(historyService, processInstanceId, Constants.VAR_USER).getValue();
        if (user == null) {
            user = (String) getHistoricVarInstanceValue(historyService, processInstanceId, Constants.PARAM_INITIATOR).getValue();
        }
        return user;
    }

    public static HistoricVariableInstance getHistoricVarInstanceValue(HistoryService historyService, String processInstanceId,
        String parameter) {
        return historyService.createHistoricVariableInstanceQuery()
            .processInstanceId(processInstanceId)
            .variableName(parameter)
            .singleResult();
    }
}
