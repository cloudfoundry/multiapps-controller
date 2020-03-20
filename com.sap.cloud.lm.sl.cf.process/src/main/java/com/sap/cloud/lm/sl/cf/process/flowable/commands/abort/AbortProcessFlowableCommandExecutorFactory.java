package com.sap.cloud.lm.sl.cf.process.flowable.commands.abort;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.flowable.ProcessActionRegistry;
import com.sap.cloud.lm.sl.cf.process.flowable.commands.FlowableCommandExecutor;
import com.sap.cloud.lm.sl.cf.process.flowable.commands.FlowableCommandExecutorFactory;
import com.sap.cloud.lm.sl.cf.process.util.HistoryUtil;
import org.flowable.common.engine.impl.interceptor.CommandContext;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class AbortProcessFlowableCommandExecutorFactory implements FlowableCommandExecutorFactory {

    private ProcessActionRegistry processActionRegistry;

    @Inject
    public AbortProcessFlowableCommandExecutorFactory(ProcessActionRegistry processActionRegistry) {
        this.processActionRegistry = processActionRegistry;
    }

    @Override
    public FlowableCommandExecutor getExecutor(CommandContext commandContext, String processInstanceId) {
        String correlationId = HistoryUtil.getVariableValue(commandContext, processInstanceId, Constants.VAR_CORRELATION_ID);
        return new AbortProcessFlowableCommandExecutor(processActionRegistry, correlationId);
    }
}
