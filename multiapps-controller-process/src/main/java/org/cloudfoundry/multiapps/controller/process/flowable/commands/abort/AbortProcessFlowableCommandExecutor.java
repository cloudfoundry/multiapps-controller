package org.cloudfoundry.multiapps.controller.process.flowable.commands.abort;

import java.text.MessageFormat;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.flowable.Action;
import org.cloudfoundry.multiapps.controller.process.flowable.ProcessAction;
import org.cloudfoundry.multiapps.controller.process.flowable.ProcessActionRegistry;
import org.cloudfoundry.multiapps.controller.process.flowable.commands.FlowableCommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbortProcessFlowableCommandExecutor implements FlowableCommandExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbortProcessFlowableCommandExecutor.class);
    private final ProcessActionRegistry processActionRegistry;
    private final String processInstanceId;

    public AbortProcessFlowableCommandExecutor(ProcessActionRegistry processActionRegistry, String processInstanceId) {
        this.processActionRegistry = processActionRegistry;
        this.processInstanceId = processInstanceId;
    }

    @Override
    public void executeCommand() {
        LOGGER.info(MessageFormat.format(Messages.AUTO_ABORTING_PROCESS_0, processInstanceId));
        ProcessAction abortProcessAction = processActionRegistry.getAction(Action.ABORT);
        abortProcessAction.execute(null, processInstanceId);
    }
}
