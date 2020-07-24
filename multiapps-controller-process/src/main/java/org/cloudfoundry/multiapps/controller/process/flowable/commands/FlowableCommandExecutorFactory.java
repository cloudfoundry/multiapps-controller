package org.cloudfoundry.multiapps.controller.process.flowable.commands;

import org.flowable.common.engine.impl.interceptor.CommandContext;

public interface FlowableCommandExecutorFactory {

    FlowableCommandExecutor getExecutor(CommandContext commandContext, String processInstanceId);
}
