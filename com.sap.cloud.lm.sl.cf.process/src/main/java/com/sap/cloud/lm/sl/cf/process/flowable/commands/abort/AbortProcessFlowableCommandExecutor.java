package com.sap.cloud.lm.sl.cf.process.flowable.commands.abort;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.flowable.AbortProcessAction;
import com.sap.cloud.lm.sl.cf.process.flowable.ProcessAction;
import com.sap.cloud.lm.sl.cf.process.flowable.ProcessActionRegistry;
import com.sap.cloud.lm.sl.cf.process.flowable.commands.FlowableCommandExecutor;

public class AbortProcessFlowableCommandExecutor implements FlowableCommandExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbortProcessFlowableCommandExecutor.class);
    private ProcessActionRegistry processActionRegistry;
    private String processInstanceId;

    public AbortProcessFlowableCommandExecutor(ProcessActionRegistry processActionRegistry, String processInstanceId) {
        this.processActionRegistry = processActionRegistry;
        this.processInstanceId = processInstanceId;
    }

    @Override
    public void executeCommand() {
        LOGGER.info(MessageFormat.format(Messages.AUTO_ABORTING_PROCESS_0, processInstanceId));
        ProcessAction abortProcessAction = processActionRegistry.getAction(AbortProcessAction.ACTION_ID_ABORT);
        abortProcessAction.execute(null, processInstanceId);
    }
}
