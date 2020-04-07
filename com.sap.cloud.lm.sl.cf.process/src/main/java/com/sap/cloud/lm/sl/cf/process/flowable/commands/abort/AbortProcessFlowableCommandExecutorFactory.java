package com.sap.cloud.lm.sl.cf.process.flowable.commands.abort;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.common.engine.impl.interceptor.CommandContext;

import com.sap.cloud.lm.sl.cf.process.flowable.ProcessActionRegistry;
import com.sap.cloud.lm.sl.cf.process.flowable.commands.FlowableCommandExecutor;
import com.sap.cloud.lm.sl.cf.process.flowable.commands.FlowableCommandExecutorFactory;
import com.sap.cloud.lm.sl.cf.process.util.HistoryUtil;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

@Named
public class AbortProcessFlowableCommandExecutorFactory implements FlowableCommandExecutorFactory {

    private ProcessActionRegistry processActionRegistry;

    @Inject
    public AbortProcessFlowableCommandExecutorFactory(ProcessActionRegistry processActionRegistry) {
        this.processActionRegistry = processActionRegistry;
    }

    @Override
    public FlowableCommandExecutor getExecutor(CommandContext commandContext, String processInstanceId) {
        String correlationId = HistoryUtil.getVariableValue(commandContext, processInstanceId, Variables.CORRELATION_ID.getName());
        return new AbortProcessFlowableCommandExecutor(processActionRegistry, correlationId);
    }
}
