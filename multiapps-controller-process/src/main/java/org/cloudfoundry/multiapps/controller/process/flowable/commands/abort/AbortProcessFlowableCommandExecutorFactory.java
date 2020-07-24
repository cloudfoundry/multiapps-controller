package org.cloudfoundry.multiapps.controller.process.flowable.commands.abort;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.process.flowable.ProcessActionRegistry;
import org.cloudfoundry.multiapps.controller.process.flowable.commands.FlowableCommandExecutor;
import org.cloudfoundry.multiapps.controller.process.flowable.commands.FlowableCommandExecutorFactory;
import org.cloudfoundry.multiapps.controller.process.util.HistoryUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.common.engine.impl.interceptor.CommandContext;

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
