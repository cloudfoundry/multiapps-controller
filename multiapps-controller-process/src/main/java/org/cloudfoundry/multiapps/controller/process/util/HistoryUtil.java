package org.cloudfoundry.multiapps.controller.process.util;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.HistoryService;
import org.flowable.engine.impl.context.Context;
import org.flowable.variable.api.history.HistoricVariableInstance;

public final class HistoryUtil {

    private HistoryUtil() {
    }

    public static <T> T getVariableValue(CommandContext commandContext, String processInstanceId, String variableName) {
        HistoricVariableInstance variable = getVariableInstance(commandContext, processInstanceId, variableName);
        return getValue(variable);
    }

    public static <T> T getVariableValue(HistoryService historyService, String processInstanceId, String variableName) {
        HistoricVariableInstance variable = getVariableInstance(historyService, processInstanceId, variableName);
        return getValue(variable);
    }

    public static HistoricVariableInstance getVariableInstance(CommandContext commandContext, String processInstanceId,
                                                               String variableName) {
        return getVariableInstance(getHistoryService(commandContext), processInstanceId, variableName);
    }

    public static HistoricVariableInstance getVariableInstance(HistoryService historyService, String processInstanceId,
                                                               String variableName) {
        return historyService.createHistoricVariableInstanceQuery()
                             .processInstanceId(processInstanceId)
                             .variableName(variableName)
                             .singleResult();
    }

    private static HistoryService getHistoryService(CommandContext commandContext) {
        return Context.getProcessEngineConfiguration(commandContext)
                      .getHistoryService();
    }

    @SuppressWarnings("unchecked")
    private static <T> T getValue(HistoricVariableInstance variable) {
        return variable == null ? null : (T) variable.getValue();
    }

}
